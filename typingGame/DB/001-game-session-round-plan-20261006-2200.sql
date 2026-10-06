-- 变更编号: 001
-- 变更时间: 2026-10-06 22:00
-- 目的: 为游戏会话、轮次和服务端 RoundPlan 建立可追踪的持久化基础。
-- 说明: 本脚本只新增表和兼容字段，不删除或重命名现有字段。
-- 执行前: 备份 game_record、monster_data，并确认数据库字符集为 utf8mb4。

SET @db_name = DATABASE();

CREATE TABLE IF NOT EXISTS game_session (
    session_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '游戏会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    game_mode VARCHAR(30) NOT NULL COMMENT '游戏模式',
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/COMPLETED/ABORTED',
    current_round INT NOT NULL DEFAULT 1 COMMENT '当前轮次',
    start_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time DATETIME NULL COMMENT '结束时间',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_game_session_user FOREIGN KEY (user_id) REFERENCES user_info(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏会话表';

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'game_session' AND index_name = 'idx_game_session_user_time') = 0,
    'CREATE INDEX idx_game_session_user_time ON game_session(user_id, created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'game_session' AND index_name = 'idx_game_session_user_status') = 0,
    'CREATE INDEX idx_game_session_user_status ON game_session(user_id, status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS game_round (
    round_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '游戏轮次ID',
    session_id BIGINT NOT NULL COMMENT '游戏会话ID',
    round_num INT NOT NULL COMMENT '轮次编号',
    plan_version BIGINT NOT NULL COMMENT 'RoundPlan版本',
    formula_version VARCHAR(32) NOT NULL DEFAULT 'v1' COMMENT '难度公式版本',
    difficulty_before DECIMAL(6,4) NULL COMMENT '轮前难度，0~1',
    difficulty_after DECIMAL(6,4) NULL COMMENT '轮后难度，0~1',
    target_accuracy DECIMAL(6,4) NOT NULL DEFAULT 0.8500 COMMENT '目标准确率，0~1',
    actual_accuracy DECIMAL(6,4) NULL COMMENT '实际准确率，0~1',
    avg_speed DECIMAL(8,2) NULL COMMENT '平均速度，字符/分钟',
    monster_speed DECIMAL(8,3) NOT NULL COMMENT '怪物速度，像素/秒',
    monster_count INT NOT NULL COMMENT '主怪物数量',
    string_length INT NOT NULL COMMENT '目标字符串长度',
    normal_ratio INT NOT NULL DEFAULT 80 COMMENT '普通怪物比例',
    attacker_ratio INT NOT NULL DEFAULT 5 COMMENT '攻击怪物比例',
    evasive_ratio INT NOT NULL DEFAULT 10 COMMENT '回避怪物比例',
    smart_ratio INT NOT NULL DEFAULT 5 COMMENT '智慧怪物比例',
    plan_payload LONGTEXT NULL COMMENT '本轮怪物计划JSON快照',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/RUNNING/COMPLETED/ABORTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    completed_at DATETIME NULL COMMENT '完成时间',
    CONSTRAINT uk_game_round_session_num UNIQUE (session_id, round_num),
    CONSTRAINT uk_game_round_plan UNIQUE (session_id, round_num, plan_version),
    CONSTRAINT fk_game_round_session FOREIGN KEY (session_id) REFERENCES game_session(session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏轮次和RoundPlan表';

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db_name AND table_name = 'game_round' AND column_name = 'plan_payload') = 0,
    'ALTER TABLE game_round ADD COLUMN plan_payload LONGTEXT NULL COMMENT ''本轮怪物计划JSON快照''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'game_round' AND index_name = 'idx_game_round_session_status') = 0,
    'CREATE INDEX idx_game_round_session_status ON game_round(session_id, status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 兼容扩展 game_record。历史数据允许为空，新链路逐步回填。
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'game_record' AND column_name = 'session_id') = 0,
    'ALTER TABLE game_record ADD COLUMN session_id BIGINT NULL COMMENT ''关联游戏会话ID''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'game_record' AND column_name = 'last_round_id') = 0,
    'ALTER TABLE game_record ADD COLUMN last_round_id BIGINT NULL COMMENT ''最后完成轮次ID''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'game_record' AND column_name = 'accuracy_ratio') = 0,
    'ALTER TABLE game_record ADD COLUMN accuracy_ratio DECIMAL(6,4) NULL COMMENT ''准确率，0~1''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'game_record' AND column_name = 'game_duration_ms') = 0,
    'ALTER TABLE game_record ADD COLUMN game_duration_ms BIGINT NULL COMMENT ''游戏时长，毫秒''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'game_record' AND column_name = 'data_version') = 0,
    'ALTER TABLE game_record ADD COLUMN data_version INT NOT NULL DEFAULT 1 COMMENT ''数据口径版本''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'game_record' AND index_name = 'idx_game_record_session') = 0,
    'CREATE INDEX idx_game_record_session ON game_record(session_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 兼容扩展 monster_data。旧字段保留，新字段由新记录链路写入。
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'monster_data' AND column_name = 'round_id') = 0,
    'ALTER TABLE monster_data ADD COLUMN round_id BIGINT NULL COMMENT ''关联游戏轮次ID''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'monster_data' AND column_name = 'input_count') = 0,
    'ALTER TABLE monster_data ADD COLUMN input_count INT NULL COMMENT ''输入总次数''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'monster_data' AND column_name = 'correct_count') = 0,
    'ALTER TABLE monster_data ADD COLUMN correct_count INT NULL COMMENT ''正确输入次数''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'monster_data' AND column_name = 'error_count') = 0,
    'ALTER TABLE monster_data ADD COLUMN error_count INT NULL COMMENT ''错误输入次数''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'monster_data' AND column_name = 'result') = 0,
    'ALTER TABLE monster_data ADD COLUMN result VARCHAR(20) NULL COMMENT ''KILLED/MISSED/EVADED/COLLISION/TIMEOUT''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'monster_data' AND column_name = 'generation') = 0,
    'ALTER TABLE monster_data ADD COLUMN generation INT NULL COMMENT ''遗传代数''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'monster_data' AND column_name = 'plan_version') = 0,
    'ALTER TABLE monster_data ADD COLUMN plan_version BIGINT NULL COMMENT ''RoundPlan版本''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'monster_data' AND index_name = 'idx_monster_round') = 0,
    'CREATE INDEX idx_monster_round ON monster_data(round_id, monster_type)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 初始兼容回填：旧记录的准确率从百分数转换为0~1，新字段不覆盖已有非空值。
UPDATE game_record
SET accuracy_ratio = LEAST(1.0000, GREATEST(0.0000, avg_accuracy / 100.0)),
    game_duration_ms = game_duration * 1000
WHERE accuracy_ratio IS NULL;

UPDATE monster_data
SET input_count = COALESCE(total_count, 0),
    error_count = COALESCE(input_error_count, 0),
    correct_count = GREATEST(0, COALESCE(total_count, 0) - COALESCE(input_error_count, 0))
WHERE input_count IS NULL;

-- 验证：应能查询到新增表和兼容字段。
SELECT table_name FROM information_schema.tables
WHERE table_schema = @db_name AND table_name IN ('game_session', 'game_round');

SELECT table_name, column_name FROM information_schema.columns
WHERE table_schema = @db_name
  AND ((table_name = 'game_record' AND column_name IN ('session_id', 'last_round_id', 'accuracy_ratio', 'game_duration_ms', 'data_version'))
    OR (table_name = 'monster_data' AND column_name IN ('round_id', 'input_count', 'correct_count', 'error_count', 'result', 'generation', 'plan_version')))
ORDER BY table_name, ordinal_position;

-- 回滚说明（需人工确认后执行，避免误删业务数据）：
-- DROP TABLE IF EXISTS game_round;
-- DROP TABLE IF EXISTS game_session;
-- ALTER TABLE game_record DROP COLUMN session_id, DROP COLUMN last_round_id,
--   DROP COLUMN accuracy_ratio, DROP COLUMN game_duration_ms, DROP COLUMN data_version;
-- ALTER TABLE monster_data DROP COLUMN round_id, DROP COLUMN input_count,
--   DROP COLUMN correct_count, DROP COLUMN error_count, DROP COLUMN result,
--   DROP COLUMN generation, DROP COLUMN plan_version;
