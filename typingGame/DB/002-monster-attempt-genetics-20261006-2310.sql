-- 变更编号: 002
-- 变更时间: 2026-10-06 23:10
-- 目的: 保存轮次内怪物尝试结果，并为无尽模式父子代关系提供结构化存储。
-- 执行前: 先执行001脚本并备份 game_round、monster_data。

SET @db_name = DATABASE();

CREATE TABLE IF NOT EXISTS monster_attempt (
    attempt_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '怪物尝试ID',
    session_id BIGINT NOT NULL COMMENT '游戏会话ID',
    round_id BIGINT NOT NULL COMMENT '游戏轮次ID',
    plan_version BIGINT NOT NULL COMMENT 'RoundPlan版本',
    monster_key VARCHAR(100) NULL COMMENT '客户端怪物标识',
    target_str VARCHAR(200) NOT NULL COMMENT '目标字符串',
    monster_type VARCHAR(20) NOT NULL COMMENT '怪物类型',
    speed DECIMAL(8,3) NULL COMMENT '怪物速度，像素/秒',
    input_count INT NOT NULL DEFAULT 0 COMMENT '输入总次数',
    correct_count INT NOT NULL DEFAULT 0 COMMENT '正确输入次数',
    error_count INT NOT NULL DEFAULT 0 COMMENT '错误输入次数',
    result VARCHAR(20) NOT NULL COMMENT 'KILLED/MISSED/EVADED/COLLISION/TIMEOUT/ABANDONED',
    client_score INT NULL COMMENT '客户端上报分数，仅用于审计',
    verified_score INT NOT NULL DEFAULT 0 COMMENT '服务端核验分数',
    fitness DECIMAL(10,4) NULL COMMENT '父本适应度',
    generation INT NOT NULL DEFAULT 0 COMMENT '遗传代数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    eliminated_at DATETIME NULL COMMENT '结束时间',
    CONSTRAINT uk_monster_attempt_key UNIQUE (round_id, monster_key),
    CONSTRAINT fk_monster_attempt_session FOREIGN KEY (session_id) REFERENCES game_session(session_id),
    CONSTRAINT fk_monster_attempt_round FOREIGN KEY (round_id) REFERENCES game_round(round_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮次怪物尝试结果';

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'monster_attempt' AND index_name = 'idx_monster_attempt_session') = 0,
    'CREATE INDEX idx_monster_attempt_session ON monster_attempt(session_id, created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'monster_attempt' AND index_name = 'idx_monster_attempt_round_result') = 0,
    'CREATE INDEX idx_monster_attempt_round_result ON monster_attempt(round_id, result)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS monster_parent_relation (
    relation_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '父子关系ID',
    child_attempt_id BIGINT NOT NULL COMMENT '子代怪物尝试ID',
    parent_a_attempt_id BIGINT NULL COMMENT '父本A尝试ID',
    parent_b_attempt_id BIGINT NULL COMMENT '父本B尝试ID',
    crossover_point INT NULL COMMENT '交叉位置',
    mutation_rate DECIMAL(6,4) NULL COMMENT '变异率',
    mutation_positions VARCHAR(255) NULL COMMENT '变异位置列表',
    mutation_chars VARCHAR(255) NULL COMMENT '变异字符列表',
    generation INT NOT NULL DEFAULT 0 COMMENT '遗传代数',
    algorithm_version VARCHAR(32) NOT NULL DEFAULT 'v1' COMMENT '遗传算法版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT uk_parent_relation_child UNIQUE (child_attempt_id),
    CONSTRAINT fk_parent_relation_child FOREIGN KEY (child_attempt_id) REFERENCES monster_attempt(attempt_id),
    CONSTRAINT fk_parent_relation_a FOREIGN KEY (parent_a_attempt_id) REFERENCES monster_attempt(attempt_id),
    CONSTRAINT fk_parent_relation_b FOREIGN KEY (parent_b_attempt_id) REFERENCES monster_attempt(attempt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='怪物遗传父子关系';

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'monster_parent_relation' AND index_name = 'idx_parent_relation_parent_a') = 0,
    'CREATE INDEX idx_parent_relation_parent_a ON monster_parent_relation(parent_a_attempt_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'monster_parent_relation' AND index_name = 'idx_parent_relation_parent_b') = 0,
    'CREATE INDEX idx_parent_relation_parent_b ON monster_parent_relation(parent_b_attempt_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 验证
SELECT table_name FROM information_schema.tables
WHERE table_schema = @db_name AND table_name IN ('monster_attempt', 'monster_parent_relation');

-- 回滚说明：先删除关系表，再删除尝试表。
-- DROP TABLE IF EXISTS monster_parent_relation;
-- DROP TABLE IF EXISTS monster_attempt;
