package com.typinggame.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 好友聊天消息表
 */
@Data
@Entity
@Table(name = "friend_chat_message")
public class FriendChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "conversation_id", nullable = false)
    private Integer conversationId;

    @Column(name = "sender_id", nullable = false)
    private Integer senderId;

    @Column(name = "receiver_id", nullable = false)
    private Integer receiverId;

    @Column(name = "message_type", nullable = false)
    private Integer messageType;

    @Column(name = "message_content", length = 1000, nullable = false)
    private String messageContent;

    @Column(name = "client_msg_id", length = 64)
    private String clientMsgId;

    @Column(name = "server_seq")
    private Integer serverSeq;

    @Column(name = "send_time", nullable = false)
    private LocalDateTime sendTime;

    @Column(name = "read_status", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private Integer readStatus = 0;

    @Column(name = "read_time")
    private LocalDateTime readTime;

    @Column(name = "is_recall", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isRecall = false;

    @Column(name = "recall_time")
    private LocalDateTime recallTime;

    @Column(name = "create_time", insertable = true, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @CreationTimestamp
    private LocalDateTime createTime;
}