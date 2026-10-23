-- 把已发布的难度规则改成准确率调节器。可重复执行。
-- 旧的复合指标把速度、轮次和镇定项加进同一个设定点，准确率均衡不再是 0.85。
-- 回滚：用迁移前的 game_rule_config.payload 覆盖 active 行。

UPDATE game_rule_config
SET payload = JSON_SET(
        COALESCE(payload, JSON_OBJECT()),
        '$.weightAccuracy', 1,
        '$.weightNormalizedSpeed', 0,
        '$.weightHistorySpeed', 0,
        '$.weightComposure', 1,
        '$.weightRound', 0,
        '$.speedSpan', 1,
        '$.countBase', 3,
        '$.countRoundFactor', 6,
        '$.countDifficultyFactor', 16,
        '$.normalSlope', 0.45,
        '$.normalMin', 35,
        '$.attackSlope', 0.15,
        '$.attackMax', 20,
        '$.evasiveBase', 8,
        '$.evasiveSlope', 0.12,
        '$.evasiveMin', 5,
        '$.evasiveMax', 25
    )
WHERE rule_key = 'difficulty' AND status = 'active';
