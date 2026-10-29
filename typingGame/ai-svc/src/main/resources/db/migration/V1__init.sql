-- =============================================================================
-- ai-svc / typing_game_ai 初始 schema（Flyway V1）
-- 来源：DB/typing_word_game.sql（2026-10-30 合并版）按实体归属切分，共 2 张表
-- 跨 schema 外键已降级为逻辑引用（服务间通过 /api/v1/internal/** 校验），
-- 同 schema 外键保留在本文件末尾。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ai_conversation;
CREATE TABLE ai_conversation (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    ai_robot_id INT NOT NULL COMMENT 'AI机器人ID 区分不同功能的AI 1-打字陪练 2-问题解答 3-单词学习',
    conversation_title VARCHAR(100) NULL COMMENT '会话标题 自动提取第一条消息或用户自定义',
    last_message_id INT NULL COMMENT '最后一条消息ID',
    last_message_time DATETIME NULL COMMENT '最后一条消息时间',
    is_top TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶 0-否 1-是',
    is_mute TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否静音 0-否 1-是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '会话更新时间',
    UNIQUE KEY uk_ai_conversation_user_robot_title (user_id, ai_robot_id, conversation_title),
    KEY idx_ai_conversation_robot (ai_robot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与AI的对话会话表';

DROP TABLE IF EXISTS ai_chat_message;
CREATE TABLE ai_chat_message (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id INT NOT NULL COMMENT '关联的AI会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    ai_robot_id INT NOT NULL COMMENT 'AI机器人ID',
    message_role INT NOT NULL COMMENT '消息角色 1-用户 2-AI助手',
    message_type INT NOT NULL COMMENT '消息类型 1-文本 2-打字练习指令 3-单词查询 4-错误反馈',
    message_content TEXT NOT NULL COMMENT '消息内容 文本/指令参数JSON',
    send_time DATETIME NOT NULL COMMENT '消息发送时间',
    is_success TINYINT(1) NULL COMMENT 'AI指令执行状态 仅类型2/3有效 0-失败 1-成功 NULL-无指令',
    error_msg VARCHAR(200) NULL COMMENT '指令执行失败原因 is_success为0时必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    KEY idx_ai_chat_message_conversation (conversation_id),
    KEY idx_ai_chat_message_role_time (message_role, send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与AI的聊天消息表';

SET FOREIGN_KEY_CHECKS = 1;

-- 同 schema 外键
ALTER TABLE ai_chat_message ADD CONSTRAINT fk_ai_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id) ON DELETE CASCADE;

