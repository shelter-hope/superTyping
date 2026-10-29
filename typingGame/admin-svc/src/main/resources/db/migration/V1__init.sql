-- =============================================================================
-- admin-svc / typing_game_admin 初始 schema（Flyway V1）
-- 来源：DB/typing_word_game.sql（2026-10-30 合并版）按实体归属切分，共 1 张表
-- 跨 schema 外键已降级为逻辑引用（服务间通过 /api/v1/internal/** 校验），
-- 同 schema 外键保留在本文件末尾。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS admin_operation_log;
CREATE TABLE admin_operation_log (
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
    KEY idx_admin_operation_log_operator (operator_id, create_time),
    KEY idx_admin_operation_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台操作日志';

SET FOREIGN_KEY_CHECKS = 1;

