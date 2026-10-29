package com.typinggame.ai.entity;

import com.typinggame.entity.user.UserInfo;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * AI聊天消息表实体类
 */
@Data
@Entity
@Table(name = "ai_chat_message")
public class AiChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ai_chat_message_conversation"))
    private AiConversation conversation;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ai_chat_message_user"))
    private UserInfo user;

    @Column(name = "ai_robot_id", nullable = false)
    private Integer aiRobotId;

    @Column(name = "message_role", nullable = false)
    private Integer messageRole;

    @Column(name = "message_type", nullable = false)
    private Integer messageType;

    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @Column(name = "send_time", nullable = false)
    private Date sendTime;

    @Column(name = "is_success")
    private Boolean isSuccess;

    @Column(name = "error_msg")
    private String errorMsg;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private Date createTime;
}
