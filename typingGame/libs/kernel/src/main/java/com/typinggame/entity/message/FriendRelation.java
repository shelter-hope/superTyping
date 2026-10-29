package com.typinggame.entity.message;

import com.typinggame.entity.user.UserInfo;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 用户好友关系表
 */
@Data
@Entity
@Table(name = "friend_relation")
public class FriendRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Comment("主键ID")
    private Integer id;

    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_friend_relation_user"))
    @Comment("发起好友申请的用户ID")
    private Integer userId;

    @JoinColumn(name = "friend_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_friend_relation_friend"))
    @Comment("接收好友申请的用户ID")            
    private Integer friendUserId;

    @Column(name = "relation_status", nullable = false)
    @Comment("关系状态 0-申请中 1-已成为好友 2-已拉黑 3-已拒绝")
    private Integer relationStatus;

    @Column(name = "apply_message", length = 200)
    @Comment("申请附言")
    private String applyMessage;

    @Column(name = "blocked_by")
    @Comment("拉黑方用户ID，仅 relation_status=2 时有值")
    private Integer blockedBy;

    @Column(name = "apply_time", nullable = false)
    @Comment("申请时间")
    private LocalDateTime applyTime;

    @Column(name = "agree_time")
    @Comment("同意时间 状态为1时必填")
    private LocalDateTime agreeTime;

    @Column(name = "create_time", insertable = true, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("记录创建时间")
    @CreationTimestamp
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = true, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @CreationTimestamp
    private LocalDateTime updateTime;

}