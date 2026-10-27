-- 遗传算法 v2：精英保留 + 锦标赛选择 + 双点交叉。可重复执行。
-- 旧 payload 里没有这些字段时引擎取 Java 默认值；这里显式写入 active 行，
-- 让管理台看得见、也保证同一份历史与配置能复现同一批怪物。
-- eliteRatio=0.1：每代按适应度原样保留 10% 的父本；tournamentSize=3：每次抽 3 个父本取最好的。
-- 回滚：把上一版 game_rule_config 的 status 改回 active，或用迁移前的 payload 覆盖本行。

UPDATE game_rule_config
SET payload = JSON_SET(
        COALESCE(payload, JSON_OBJECT()),
        '$.crossover', 'TWO_POINT',
        '$.eliteRatio', 0.1,
        '$.tournamentSize', 3
    )
WHERE rule_key = 'genetic' AND status = 'active';
