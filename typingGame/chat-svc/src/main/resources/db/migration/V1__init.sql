-- =============================================================================
-- chat-svc / typing_game_chat 初始 schema（Flyway V1）
-- 来源：DB/typing_word_game.sql（2026-10-30 合并版）按实体归属切分，共 4 张表
-- 跨 schema 外键已降级为逻辑引用（服务间通过 /api/v1/internal/** 校验），
-- 同 schema 外键保留在本文件末尾。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS user_conversation;
CREATE TABLE user_conversation (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id INT NOT NULL COMMENT '当前用户ID',
    target_user_id INT NOT NULL COMMENT '对话的好友用户ID',
    last_message_id INT NULL COMMENT '最后一条消息ID',
    last_message_content VARCHAR(500) NULL COMMENT '最后一条消息内容，用于会话列表预览',
    last_message_time DATETIME NULL COMMENT '最后一条消息发送时间',
    unread_count INT NOT NULL DEFAULT 0 COMMENT '未读消息数',
    is_top TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶 0-否 1-是',
    is_mute TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否静音 0-否 1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '会话更新时间',
    UNIQUE KEY uk_user_conversation_user_target (user_id, target_user_id),
    KEY idx_user_conversation_last_msg_time (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户好友对话会话表';

DROP TABLE IF EXISTS friend_chat_message;
CREATE TABLE friend_chat_message (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id INT NOT NULL COMMENT '关联的会话ID（user_conversation.id）',
    sender_id INT NOT NULL COMMENT '消息发送者ID',
    receiver_id INT NOT NULL COMMENT '消息接收者ID',
    message_type INT NOT NULL COMMENT '消息类型 1-文本 2-表情 3-游戏邀请 4-战绩分享 5-图片 6-文件 9-系统私信',
    message_content VARCHAR(1000) NOT NULL COMMENT '消息内容 文本/表情编码/邀请参数JSON',
    client_msg_id VARCHAR(64) NULL COMMENT '客户端幂等键',
    server_seq INT NULL COMMENT '服务端序号',
    send_time DATETIME NOT NULL COMMENT '消息发送时间',
    read_status TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态 0-未读 1-已读',
    read_time DATETIME NULL COMMENT '阅读时间，状态为1时必填',
    is_recall TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否撤回 0-否 1-是',
    recall_time DATETIME NULL COMMENT '撤回时间，状态为1时必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    KEY idx_friend_chat_message_conversation_time (conversation_id, send_time),
    KEY idx_friend_chat_message_sender_client (sender_id, client_msg_id),
    KEY idx_friend_chat_message_sender_receiver (sender_id, receiver_id),
    KEY idx_friend_chat_message_receiver_read (receiver_id, read_status),
    KEY idx_friend_chat_message_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友聊天消息表';

DROP TABLE IF EXISTS chat_attachment;
CREATE TABLE chat_attachment (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '附件ID',
    owner_id INT NOT NULL COMMENT '上传者用户ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_name VARCHAR(80) NOT NULL COMMENT '磁盘文件名',
    media_type VARCHAR(100) NOT NULL COMMENT '媒体类型',
    size_bytes BIGINT NOT NULL COMMENT '字节数',
    kind INT NOT NULL COMMENT '5-图片 6-文件，与消息类型一致',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    KEY idx_chat_attachment_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天附件';

DROP TABLE IF EXISTS system_notification;
CREATE TABLE system_notification (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    user_id INT NOT NULL COMMENT '接收消息的用户ID 0表示全体用户',
    notification_type INT NOT NULL COMMENT '消息类型 1-好友申请 2-系统公告 3-游戏奖励 4-任务提醒 5-异常通知',
    notification_title VARCHAR(100) NOT NULL COMMENT '消息标题',
    notification_content VARCHAR(500) NOT NULL COMMENT '消息内容',
    business_id INT NULL COMMENT '关联业务ID 如好友申请的relation_id/奖励的record_id',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '阅读状态 0-未读 1-已读',
    is_persistent TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否持久化 0-临时消息（如弹窗） 1-持久消息（如通知列表）',
    expire_time DATETIME NULL COMMENT '消息过期时间 NULL表示永不过期',
    send_time DATETIME NOT NULL COMMENT '消息发送时间',
    read_time DATETIME NULL COMMENT '阅读时间，状态为1时必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    KEY idx_system_notification_user_read (user_id, is_read),
    KEY idx_system_notification_send_time (send_time),
    KEY idx_system_notification_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息通知表';

SET FOREIGN_KEY_CHECKS = 1;

-- 同 schema 外键
ALTER TABLE friend_chat_message ADD CONSTRAINT fk_friend_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES user_conversation(id) ON DELETE CASCADE;

