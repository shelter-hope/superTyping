-- 变更编号: 004
-- 变更时间: 2026-10-22
-- 目的: 后台管理加固
--   1. user_info 增加账号状态 status、最近登录时间 last_login_time（支持禁用用户、统计活跃）
--   2. system_params.param_value 加长，用于保存 JSON 形式的后台规则配置
--   3. 新增 admin_operation_log（后台操作日志）、config_template（后台配置模板）
--   4. friend_chat_message 增加按会话取消息、按收件人统计未读的联合索引
-- 说明: 全部语句可重复执行（幂等）。服务以 ddl-auto=update 启动时可能已建出部分列，脚本会统一修正类型与默认值。
-- 公告: 系统公告按用户逐条写入 system_notification（每人独立已读状态），因此保留 user_id 外键。

SET @db_name = DATABASE();

-- 1. user_info.status
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'user_info' AND column_name = 'status') = 0,
    'ALTER TABLE user_info ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT ''账号状态 1-正常 0-禁用'' AFTER role',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE user_info SET status = 1 WHERE status IS NULL;
ALTER TABLE user_info MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态 1-正常 0-禁用';

-- 1. user_info.last_login_time
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'user_info' AND column_name = 'last_login_time') = 0,
    'ALTER TABLE user_info ADD COLUMN last_login_time DATETIME NULL COMMENT ''最近登录时间'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = @db_name AND table_name = 'user_info' AND index_name = 'idx_user_last_login') = 0,
    'CREATE INDEX idx_user_last_login ON user_info(last_login_time)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. system_params.param_value 加长（只加长，不缩短）
SET @sql = IF(
    (SELECT COALESCE(MAX(character_maximum_length), 0) FROM information_schema.columns
      WHERE table_schema = @db_name AND table_name = 'system_params' AND column_name = 'param_value') < 2000,
    'ALTER TABLE system_params MODIFY COLUMN param_value VARCHAR(2000) NOT NULL COMMENT ''参数值（json 类型为 JSON 字符串）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 后台操作日志
CREATE TABLE IF NOT EXISTS admin_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operator_id INT NULL COMMENT '操作人用户ID',
    method VARCHAR(10) NOT NULL COMMENT 'HTTP 方法',
    path VARCHAR(255) NOT NULL COMMENT '接口路径',
    query_string VARCHAR(500) NULL COMMENT '查询参数',
    request_body TEXT NULL COMMENT '请求体（截断至 2000 字符）',
    response_status INT NULL COMMENT '响应状态码',
    success TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否成功 1-是 0-否',
    ip VARCHAR(64) NULL COMMENT '客户端 IP',
    duration_ms INT NULL COMMENT '耗时（毫秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_admin_log_operator (operator_id, create_time),
    INDEX idx_admin_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台操作日志';

-- 3. 后台配置模板
CREATE TABLE IF NOT EXISTS config_template (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    config_json TEXT NOT NULL COMMENT '配置快照 JSON',
    created_by INT NULL COMMENT '创建人用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_template_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台配置模板';

-- 4. 聊天消息联合索引
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = @db_name AND table_name = 'friend_chat_message' AND index_name = 'idx_fcm_conversation_time') = 0,
    'CREATE INDEX idx_fcm_conversation_time ON friend_chat_message(conversation_id, send_time)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = @db_name AND table_name = 'friend_chat_message' AND index_name = 'idx_fcm_receiver_read') = 0,
    'CREATE INDEX idx_fcm_receiver_read ON friend_chat_message(receiver_id, read_status)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 验证
SELECT column_name, column_type, column_default
  FROM information_schema.columns
 WHERE table_schema = @db_name AND table_name = 'user_info' AND column_name IN ('status', 'last_login_time');
SELECT table_name FROM information_schema.tables
 WHERE table_schema = @db_name AND table_name IN ('admin_operation_log', 'config_template');
