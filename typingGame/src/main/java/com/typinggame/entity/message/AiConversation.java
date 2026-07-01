package com.typinggame.entity.message;

import com.typinggame.entity.user.UserInfo;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

/**
 * AI对话会话表实体类
 */
@Data
@Entity
@Table(name = "ai_conversation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_ai", columnNames = {"user_id", "ai_robot_id", "conversation_title"})
})
public class AiConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ai_conversation_user"))
    private UserInfo user;

    @Column(name = "ai_robot_id", nullable = false)
    private Integer aiRobotId;

    @Column(name = "conversation_title")
    private String conversationTitle;

    @Column(name = "last_message_id")
    private Integer lastMessageId;

    @Column(name = "last_message_time")
    private Date lastMessageTime;

    @Column(name = "is_top", nullable = false)
    private Boolean isTop;

    @Column(name = "is_mute", nullable = false)
    private Boolean isMute;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private Date updateTime;
}
