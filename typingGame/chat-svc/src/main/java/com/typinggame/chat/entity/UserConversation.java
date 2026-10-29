package com.typinggame.chat.entity;

import com.typinggame.entity.user.UserInfo;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 用户好友对话会话表
 */
@Data
@Entity
@Table(name = "user_conversation")
public class UserConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_conversation_user"))
    @Comment("当前用户ID")
    private Integer userId;

    @JoinColumn(name = "target_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_conversation_target"))
    @Comment("对话的好友用户ID")
    private Integer targetUserId;

    @Column(name = "last_message_id")
    @Comment("最后一条消息ID")
    private Integer lastMessageId;

    @Column(name = "last_message_content", length = 500)
    @Comment("最后一条消息内容 用于会话列表预览")
    private String lastMessageContent;

    @Column(name = "last_message_time")
    @Comment("最后一条消息发送时间")
    private LocalDateTime lastMessageTime;

    @Column(name = "unread_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Comment("未读消息数")
    private Integer unreadCount = 0;

    @Column(name = "is_top", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Comment("是否置顶 0-否 1-是")
    private Boolean isTop = false;

    @Column(name = "is_mute", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Comment("是否静音 0-否 1-是")
    private Boolean isMute = false;

    @Column(name = "create_time", insertable = true, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("会话创建时间")
    @CreationTimestamp
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = true, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Comment("会话更新时间")
    @CreationTimestamp
    private LocalDateTime updateTime;


}