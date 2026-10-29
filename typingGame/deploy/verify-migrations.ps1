#requires -Version 5.1
<#
.SYNOPSIS
    空库执行各服务 Flyway V1 迁移并校验收敛结果（P4-4 的自动化闸门）。

.DESCRIPTION
    对 7 个服务逐个：建一个临时 schema → 执行该服务的 V1__init.sql → 用 information_schema
    统计实际表数 → 与脚本内 CREATE TABLE 数量比对 → 再断言脚本里没有跨 schema 的 REFERENCES。
    全部通过后删除临时 schema（-Keep 保留现场）。

    只要本脚本通过，就可以确认「空库 + 服务自举」这条路是通的；它不依赖本机已有数据，
    也不会碰任何 typing_game_* 业务 schema。

.EXAMPLE
    pwsh ./deploy/verify-migrations.ps1
    pwsh ./deploy/verify-migrations.ps1 -MysqlExe 'E:\MySQL\mysql-8.0.30-winx64\bin\mysql.exe' -Keep
#>
param(
    [string]$MysqlExe = 'E:\MySQL\mysql-8.0.30-winx64\bin\mysql.exe',
    [string]$DbHost = '127.0.0.1',
    [int]$DbPort = 3306,
    [string]$DbUser = 'root',
    [string]$DbPassword = '123456',
    [switch]$Keep
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path $MysqlExe)) { throw "mysql 客户端不存在: $MysqlExe" }

# 服务 → 迁移脚本目录（与各服务 application.yml 的 MYSQL_URL 一致）
$services = [ordered]@{
    'user'     = 'user-svc'
    'game'     = 'game-core-svc'
    'ai'       = 'ai-svc'
    'chat'     = 'chat-svc'
    'admin'    = 'admin-svc'
    'lab'      = 'lab-svc'
    'dict'     = 'dict-svc'
}

function Invoke-Mysql {
    param([string]$Schema, [string]$Sql, [string]$ImportFile)
    # 用 MYSQL_PWD 传密码，既避免命令行告警，也不会让 5.1 把 stderr 当成终止错误
    $env:MYSQL_PWD = $DbPassword
    $mysqlArgs = @("-h$DbHost", "-P$DbPort", "-u$DbUser", '-N', '--default-character-set=utf8mb4')
    if ($Schema) { $mysqlArgs += $Schema }
    if ($ImportFile) {
        $mysqlArgs += "--execute=source $ImportFile"
    } else {
        $mysqlArgs += "--execute=$Sql"
    }
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $out = & $MysqlExe @mysqlArgs 2>&1
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $prevEap
        Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    }
    if ($code -ne 0) { throw "mysql 失败 (exit $code): $out" }
    return ($out | Where-Object { $_ -notmatch 'Using a password on the command line' })
}

$failures = New-Object System.Collections.Generic.List[string]
$rows = New-Object System.Collections.Generic.List[object]

foreach ($key in $services.Keys) {
    $svc = $services[$key]
    $script = Join-Path $root "$svc/src/main/resources/db/migration/V1__init.sql"
    if (-not (Test-Path $script)) { $failures.Add("缺少迁移脚本: $svc/V1__init.sql"); continue }

    $tmp = "typing_verify_$key"
    $text = [System.IO.File]::ReadAllText($script, [System.Text.Encoding]::UTF8)

    # 脚本自身声明的表数 / 跨 schema 引用数
    $declared = ([regex]::Matches($text, '(?im)^\s*CREATE\s+TABLE\s+(IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?')).Count
    $crossRef = ([regex]::Matches($text, '(?i)REFERENCES\s+`?(\w+)`?\.')).Count
    if ($crossRef -gt 0) { $failures.Add("$svc/V1__init.sql 仍含跨 schema 外键引用 $crossRef 处") }

    try {
        Invoke-Mysql -Sql "DROP DATABASE IF EXISTS $tmp; CREATE DATABASE $tmp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | Out-Null
        Invoke-Mysql -Schema $tmp -ImportFile $script.Replace('\', '/') | Out-Null
        $actual = [int](Invoke-Mysql -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$tmp';")
        $fk = [int](Invoke-Mysql -Sql "SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_type = 'FOREIGN KEY' AND table_schema = '$tmp';")
        $seed = [int](Invoke-Mysql -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$tmp' AND table_rows > 0;")

        $status = if ($actual -eq $declared) { 'PASS' } else { 'FAIL'; }
        if ($actual -ne $declared) { $failures.Add("${svc}: 建表数与声明不一致 declared=$declared actual=$actual") }
        $rows.Add([pscustomobject]@{
                service = $svc; schema = $tmp; declared = $declared; actual = $actual
                foreignKeys = $fk; tablesWithRows = $seed; status = $status
            })
    } catch {
        $failures.Add("${svc}: $($_.Exception.Message)")
    } finally {
        if (-not $Keep) {
            try { Invoke-Mysql -Sql "DROP DATABASE IF EXISTS $tmp;" | Out-Null } catch { }
        }
    }
}

$good = $rows | Where-Object { $_.status -eq 'PASS' }
$rows | Format-Table -AutoSize | Out-String -Width 200 | Write-Host
"通过: $($good.Count) / $($services.Count) 个服务的空库迁移"

if ($failures.Count -gt 0) {
    "失败项:" | Write-Host
    $failures | ForEach-Object { "  - $_" } | Write-Host
    exit 1
}
"空库迁移校验通过" | Write-Host
exit 0
