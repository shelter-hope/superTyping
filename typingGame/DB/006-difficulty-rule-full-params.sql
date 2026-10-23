-- 难度规则全量参数补齐 + 用户难度变换日志精度/可空记录。可重复执行。
-- 回滚：手动恢复 payload 与列定义（见 typing_word_game.sql 基线）。

-- 1) active difficulty payload 补齐缺失键（含触发阈值）
UPDATE game_rule_config
SET payload = JSON_SET(
        COALESCE(payload, JSON_OBJECT()),
        '$.maxReferenceSpeed', COALESCE(JSON_EXTRACT(payload, '$.maxReferenceSpeed'), 150),
        '$.refPeakSpeed', COALESCE(JSON_EXTRACT(payload, '$.refPeakSpeed'), 120),
        '$.weakCharDivisor', COALESCE(JSON_EXTRACT(payload, '$.weakCharDivisor'), 10),
        '$.speedSpan', COALESCE(JSON_EXTRACT(payload, '$.speedSpan'), 1.8),
        '$.countBase', COALESCE(JSON_EXTRACT(payload, '$.countBase'), 4),
        '$.countRoundFactor', COALESCE(JSON_EXTRACT(payload, '$.countRoundFactor'), 0.8),
        '$.countDifficultyFactor', COALESCE(JSON_EXTRACT(payload, '$.countDifficultyFactor'), 12),
        '$.countMin', COALESCE(JSON_EXTRACT(payload, '$.countMin'), 3),
        '$.countMax', COALESCE(JSON_EXTRACT(payload, '$.countMax'), 25),
        '$.normalBase', COALESCE(JSON_EXTRACT(payload, '$.normalBase'), 80),
        '$.normalSlope', COALESCE(JSON_EXTRACT(payload, '$.normalSlope'), 0.30),
        '$.normalMin', COALESCE(JSON_EXTRACT(payload, '$.normalMin'), 50),
        '$.normalMax', COALESCE(JSON_EXTRACT(payload, '$.normalMax'), 80),
        '$.attackBase', COALESCE(JSON_EXTRACT(payload, '$.attackBase'), 5),
        '$.attackSlope', COALESCE(JSON_EXTRACT(payload, '$.attackSlope'), 0.10),
        '$.attackMin', COALESCE(JSON_EXTRACT(payload, '$.attackMin'), 5),
        '$.attackMax', COALESCE(JSON_EXTRACT(payload, '$.attackMax'), 15),
        '$.evasiveBase', COALESCE(JSON_EXTRACT(payload, '$.evasiveBase'), 10),
        '$.evasiveSlope', COALESCE(JSON_EXTRACT(payload, '$.evasiveSlope'), 0.10),
        '$.evasiveMin', COALESCE(JSON_EXTRACT(payload, '$.evasiveMin'), 10),
        '$.evasiveMax', COALESCE(JSON_EXTRACT(payload, '$.evasiveMax'), 20),
        '$.roundLogBase', COALESCE(JSON_EXTRACT(payload, '$.roundLogBase'), 32),
        '$.highAccuracyAdvanceThreshold', COALESCE(JSON_EXTRACT(payload, '$.highAccuracyAdvanceThreshold'), 85),
        '$.lowAccuracyRollbackThreshold', COALESCE(JSON_EXTRACT(payload, '$.lowAccuracyRollbackThreshold'), 50)
    )
WHERE rule_key = 'difficulty' AND status = 'active';

-- 2) 适配日志：精度、可空记录、模式/会话列
ALTER TABLE user_model_adapt_logs
    MODIFY COLUMN game_record_id INT NULL COMMENT '关联游戏记录ID（轮次日志可为 null）',
    MODIFY COLUMN before_difficulty DECIMAL(6,4) COMMENT '调整前难度系数',
    MODIFY COLUMN after_difficulty DECIMAL(6,4) COMMENT '调整后难度系数';

SET @col_game_mode := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_model_adapt_logs' AND COLUMN_NAME = 'game_mode'
);
SET @sql_game_mode := IF(@col_game_mode = 0,
    'ALTER TABLE user_model_adapt_logs ADD COLUMN game_mode VARCHAR(32) NULL COMMENT ''游戏模式'' AFTER game_record_id',
    'SELECT 1');
PREPARE stmt_game_mode FROM @sql_game_mode;
EXECUTE stmt_game_mode;
DEALLOCATE PREPARE stmt_game_mode;

SET @col_session := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_model_adapt_logs' AND COLUMN_NAME = 'session_id'
);
SET @sql_session := IF(@col_session = 0,
    'ALTER TABLE user_model_adapt_logs ADD COLUMN session_id BIGINT NULL COMMENT ''游戏会话ID'' AFTER game_mode',
    'SELECT 1');
PREPARE stmt_session FROM @sql_session;
EXECUTE stmt_session;
DEALLOCATE PREPARE stmt_session;

-- 回填已有日志的 game_mode
UPDATE user_model_adapt_logs l
    INNER JOIN game_record r ON l.game_record_id = r.record_id
SET l.game_mode = r.game_mode
WHERE l.game_mode IS NULL AND l.game_record_id IS NOT NULL;
