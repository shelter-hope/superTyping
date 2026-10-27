-- 难度模型 v2：logistic 对数几率调节器。可重复执行。
-- 旧 payload 里没有这些字段时引擎会取 Java 默认值；这里显式写进 DB，
-- 让管理台能看见并调整它们，也保证回放、审计时同一份配置能复现同一串难度。
-- 例：p* = 0.85、k = 6、上调阻尼 0.25、下调阻尼 0.5、3% 死区。
-- 回滚：用迁移前 game_rule_config.payload 覆盖 active 行（或把上一版 status 改回 active）。

UPDATE game_rule_config
SET payload = JSON_SET(
        COALESCE(payload, JSON_OBJECT()),
        '$.logitSlope', 6.0,
        '$.deadband', 0.03,
        '$.lambdaUp', 0.25,
        '$.lambdaDown', 0.5,
        '$.speedErrorScale', 2.0,
        '$.weakExposureGain', 1.0
    )
WHERE rule_key = 'difficulty' AND status = 'active';
