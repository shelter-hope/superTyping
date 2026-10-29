# typing-game 冒烟脚本（P4-3）
# 用途：compose 起栈后确认 8 个服务健康 + 关键业务链路可用
# 用法：
#   pwsh ./deploy/smoke.ps1                # 默认 127.0.0.1
#   pwsh ./deploy/smoke.ps1 -BaseUrl http://127.0.0.1:8080 -TimeoutSeconds 300
param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [int]$TimeoutSeconds = 240
)

$ErrorActionPreference = 'Continue'
$script:failures = 0

function Check([string]$name, [string]$url, [string]$expectedPattern = '', [hashtable]$headers = $null) {
    try {
        $args = @{ Uri = $url; UseBasicParsing = $true; TimeoutSec = 10 }
        if ($headers) { $args.Headers = $headers }
        $resp = Invoke-WebRequest @args
        $body = $resp.Content
        if ($expectedPattern -and $body -notmatch $expectedPattern) {
            Write-Host ("[FAIL] {0} -> {1} (body: {2})" -f $name, $resp.StatusCode, $body.Substring(0, [Math]::Min(160, $body.Length))) -ForegroundColor Red
            $script:failures++
        } else {
            Write-Host ("[ OK ] {0} -> {1}" -f $name, $resp.StatusCode) -ForegroundColor Green
        }
    } catch {
        Write-Host ("[FAIL] {0} -> {1}" -f $name, $_.Exception.Message) -ForegroundColor Red
        $script:failures++
    }
}

function CheckPost([string]$name, [string]$url, [string]$body, [string]$expectedPattern = '') {
    try {
        $resp = Invoke-WebRequest -Uri $url -Method Post -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 10
        $content = $resp.Content
        if ($expectedPattern -and $content -notmatch $expectedPattern) {
            Write-Host ("[FAIL] {0} -> {1} (body: {2})" -f $name, $resp.StatusCode, $content.Substring(0, [Math]::Min(160, $content.Length))) -ForegroundColor Red
            $script:failures++
        } else {
            Write-Host ("[ OK ] {0} -> {1}" -f $name, $resp.StatusCode) -ForegroundColor Green
        }
    } catch {
        Write-Host ("[FAIL] {0} -> {1}" -f $name, $_.Exception.Message) -ForegroundColor Red
        $script:failures++
    }
}

function Wait-Service([string]$name, [string]$url, [int]$timeout) {
    $deadline = (Get-Date).AddSeconds($timeout)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
            if ($r.StatusCode -eq 200) {
                Write-Host ("[ OK ] {0} 已就绪" -f $name) -ForegroundColor Green
                return $true
            }
        } catch { }
        Start-Sleep -Seconds 5
    }
    Write-Host ("[FAIL] {0} 在 {1}s 内未就绪：{2}" -f $name, $timeout, $url) -ForegroundColor Red
    $script:failures++
    return $false
}

$services = @(
    @{ Name = 'api-gateway';  Url = "$BaseUrl/actuator/health" },
    @{ Name = 'user-svc';     Url = 'http://127.0.0.1:8081/user-svc/actuator/health' },
    @{ Name = 'game-core-svc';Url = 'http://127.0.0.1:8082/game-core-svc/actuator/health' },
    @{ Name = 'ai-svc';       Url = 'http://127.0.0.1:8083/ai-svc/actuator/health' },
    @{ Name = 'chat-svc';     Url = 'http://127.0.0.1:8084/chat-svc/actuator/health' },
    @{ Name = 'admin-svc';    Url = 'http://127.0.0.1:8085/admin-svc/actuator/health' },
    @{ Name = 'lab-svc';      Url = 'http://127.0.0.1:8086/lab-svc/actuator/health' },
    @{ Name = 'dict-svc';     Url = 'http://127.0.0.1:8087/dict-svc/actuator/health' }
)

Write-Host "== 1/3 等待服务健康 ==" -ForegroundColor Cyan
foreach ($svc in $services) {
    [void](Wait-Service -name $svc.Name -url $svc.Url -timeout $TimeoutSeconds)
}

Write-Host "`n== 2/3 业务链路（经网关）==" -ForegroundColor Cyan
CheckPost '登录 test/test123' "$BaseUrl/api/v1/auth/login" '{"username":"test","password":"test123"}' '"accessToken"'

$token = ''
try {
    $login = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -ContentType 'application/json' `
        -Body '{"username":"test","password":"test123"}' -TimeoutSec 10
    if ($login.data.accessToken) { $token = $login.data.accessToken } elseif ($login.data.token) { $token = $login.data.token }
} catch { }

if ($token) {
    $headers = @{ Authorization = "Bearer $token" }
    Check '当前用户配置 /api/v1/user/config' "$BaseUrl/api/v1/user/config" '"userId"' $headers
    Check '排行榜 /api/v1/leaderboard'  "$BaseUrl/api/v1/leaderboard?gameMode=endless&limit=5"
    Check '词库列表 /api/v1/dictionary' "$BaseUrl/api/v1/dictionaries/system" '' $headers
    Check '游戏规则 /api/v1/game/config/char-types' "$BaseUrl/api/v1/game/config/char-types" '' $headers

    # 内部接口必须对网关不可见（安全断言）
    try {
        $internal = Invoke-WebRequest -Uri "$BaseUrl/api/v1/internal/users/1" -UseBasicParsing -TimeoutSec 5 -Headers $headers
        Write-Host ("[FAIL] 内部接口经网关返回 {0}，应被网关拦截为 404" -f $internal.StatusCode) -ForegroundColor Red
        $script:failures++
    } catch {
        $code = 0
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        if ($code -eq 404) {
            Write-Host "[ OK ] 内部接口经网关 404（符合预期）" -ForegroundColor Green
        } else {
            Write-Host ("[WARN] 内部接口经网关返回 {0}，期望 404" -f $code) -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "[WARN] 未取得 token，跳过需登录的接口" -ForegroundColor Yellow
}

Write-Host "`n== 3/3 结果 ==" -ForegroundColor Cyan
if ($script:failures -eq 0) {
    Write-Host "冒烟通过：8 个服务健康 + 核心链路可用" -ForegroundColor Green
    exit 0
} else {
    Write-Host ("冒烟失败：{0} 项" -f $script:failures) -ForegroundColor Red
    exit 1
}
