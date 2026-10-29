package com.typinggame.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 系统消息通知表
 */
@Data
@Entity
@Table(name = "system_notification")
public class SystemNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Comment("消息ID")
    private Integer id;

    @Column(name = "user_id", nullable = false)
    @Comment("接收消息的用户ID 0表示全体用户")
    private Integer userId;

    @Column(name = "notification_type", nullable = false)
    @Comment("消息类型 1-好友申请 2-系统公告 3-游戏奖励 4-任务提醒 5-异常通知")
    private Integer notificationType;

    @Column(name = "notification_title", nullable = false, length = 100)
    @Comment("消息标题")
    private String notificationTitle;

    @Column(name = "notification_content", nullable = false, length = 500)
    @Comment("消息内容")
    private String notificationContent;

    @Column(name = "business_id")
    @Comment("关联业务ID 如好友申请的relation_id/奖励的record_id")
    private Integer businessId;

    @Column(name = "is_read", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Comment("阅读状态 0-未读 1-已读")  
    private Boolean isRead = false;

    @Column(name = "is_persistent", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Comment("是否持久化 0-临时消息（如弹窗） 1-持久消息（如通知列表）")
    private Boolean isPersistent = true;

    @Column(name = "expire_time")
    @Comment("消息过期时间 NULL表示永不过期")
    private LocalDateTime expireTime;

    @Column(name = "send_time", nullable = false)
    @Comment("消息发送时间")
    private LocalDateTime sendTime;

    @Column(name = "read_time")
    @Comment("阅读时间 状态为1时必填")
    private LocalDateTime readTime;

    @Column(name = "create_time", insertable = true, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("记录创建时间")
    @CreationTimestamp
    private LocalDateTime createTime;
}