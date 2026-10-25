-- 即时通信：申请附言、拉黑方、聊天附件。可重复执行。
-- 已有库执行本脚本。新库的 typing_word_game.sql 已包含这些列和表。

SET @has_apply := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'friend_relation' AND COLUMN_NAME = 'apply_message'
);
SET @sql_apply := IF(@has_apply = 0,
    'ALTER TABLE friend_relation ADD COLUMN apply_message VARCHAR(200) NULL COMMENT ''申请附言'' AFTER relation_status',
    'SELECT 1');
PREPARE stmt FROM @sql_apply; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_blocked := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'friend_relation' AND COLUMN_NAME = 'blocked_by'
);
SET @sql_blocked := IF(@has_blocked = 0,
    'ALTER TABLE friend_relation ADD COLUMN blocked_by INT NULL COMMENT ''拉黑方用户ID'' AFTER apply_message',
    'SELECT 1');
PREPARE stmt FROM @sql_blocked; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS chat_attachment (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '附件ID',
    owner_id INT NOT NULL COMMENT '上传者用户ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_name VARCHAR(80) NOT NULL COMMENT '磁盘文件名',
    media_type VARCHAR(100) NOT NULL COMMENT '媒体类型',
    size_bytes BIGINT NOT NULL COMMENT '字节数',
    kind TINYINT NOT NULL COMMENT '5-图片 6-文件，与消息类型一致',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    KEY idx_chat_attachment_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天附件';

SET @has_fk := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_attachment' AND CONSTRAINT_NAME = 'fk_chat_attachment_owner'
);
SET @sql_fk := IF(@has_fk = 0,
    'ALTER TABLE chat_attachment ADD CONSTRAINT fk_chat_attachment_owner FOREIGN KEY (owner_id) REFERENCES user_info(user_id) ON DELETE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @sql_fk; EXECUTE stmt; DEALLOCATE PREPARE stmt;
