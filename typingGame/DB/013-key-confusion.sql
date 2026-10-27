-- 变更编号: 013
-- 变更时间: 2026-10-28
-- 目的: 新增按键混淆档案 key_confusion，记录「该打 A 却打成 B」的次数，
--       供下一局遗传算法做定向变异（GeneticEngine 的 confusion 映射）。
-- 执行前: 备份 character_stats；本脚本可重复执行。

SET @db_name = DATABASE();

CREATE TABLE IF NOT EXISTS key_confusion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id INT NOT NULL COMMENT '关联用户ID',
    expected_char VARCHAR(10) COLLATE utf8mb4_bin NOT NULL COMMENT '本该输入的字符（区分大小写）',
    typed_char VARCHAR(10) COLLATE utf8mb4_bin NOT NULL COMMENT '实际打成的字符（区分大小写）',
    confusion_count INT NOT NULL DEFAULT 0 COMMENT '累计混淆次数',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
    CONSTRAINT uk_key_confusion_pair UNIQUE (user_id, expected_char, typed_char)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='按键混淆档案';

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
        WHERE table_schema = @db_name AND table_name = 'key_confusion'
          AND index_name = 'idx_key_confusion_user_count') = 0,
    'CREATE INDEX idx_key_confusion_user_count ON key_confusion(user_id, confusion_count DESC)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 验证
SELECT COUNT(*) AS key_confusion_ready FROM information_schema.tables
WHERE table_schema = @db_name AND table_name = 'key_confusion';

-- 回滚说明：确认没有服务在读这张表后执行。
-- DROP TABLE IF EXISTS key_confusion;
