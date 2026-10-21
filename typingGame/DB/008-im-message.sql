-- 即时消息幂等键和服务端序号。可重复执行。
-- 回滚：ALTER TABLE friend_chat_message DROP COLUMN client_msg_id, DROP COLUMN server_seq;
--       ALTER TABLE user_info DROP COLUMN enabled;

SET @has_client := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'friend_chat_message' AND COLUMN_NAME = 'client_msg_id'
);
SET @sql_client := IF(@has_client = 0, 'ALTER TABLE friend_chat_message ADD COLUMN client_msg_id VARCHAR(64) NULL', 'SELECT 1');
PREPARE stmt FROM @sql_client; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_seq := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'friend_chat_message' AND COLUMN_NAME = 'server_seq'
);
SET @sql_seq := IF(@has_seq = 0, 'ALTER TABLE friend_chat_message ADD COLUMN server_seq INT NULL', 'SELECT 1');
PREPARE stmt FROM @sql_seq; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_enabled := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_info' AND COLUMN_NAME = 'enabled'
);
SET @sql_enabled := IF(@has_enabled = 0, 'ALTER TABLE user_info ADD COLUMN enabled TINYINT(1) NULL DEFAULT 1', 'SELECT 1');
PREPARE stmt FROM @sql_enabled; EXECUTE stmt; DEALLOCATE PREPARE stmt;
