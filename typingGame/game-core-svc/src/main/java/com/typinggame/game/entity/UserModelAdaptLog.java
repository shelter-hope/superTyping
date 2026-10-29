package com.typinggame.game.entity;

import com.typinggame.game.entity.GameRecord;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_model_adapt_logs")
@NoArgsConstructor
@AllArgsConstructor
public class UserModelAdaptLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id", nullable = false)
    @Comment("日志ID")
    private Integer logId;

    @Column(name = "user_id", nullable = false)
    @Comment("用户ID（仅存标量，用户主体归 user-svc）")
    private Integer userId;

    @ManyToOne
    @JoinColumn(name = "game_record_id", nullable = true, foreignKey = @ForeignKey(name = "fk_user_adapt_logs_record"))
    @Comment("关联游戏记录ID（轮次日志可为 null）")
    private GameRecord gameRecord;

    @Column(name = "game_mode", length = 32)
    @Comment("游戏模式")
    private String gameMode;

    @Column(name = "session_id")
    @Comment("游戏会话ID（轮次日志）")
    private Long sessionId;

    @Column(name = "round_num", nullable = false)
    @Comment("游戏轮次")
    private Integer roundNum;

    @Column(name = "before_difficulty", precision = 6, scale = 4)
    @Comment("调整前难度系数")
    private BigDecimal beforeDifficulty;

    @Column(name = "after_difficulty", precision = 6, scale = 4)
    @Comment("调整后难度系数")
    private BigDecimal afterDifficulty;

    @Column(name = "adjust_params", columnDefinition = "JSON")
    @Comment("调整的参数及变化")
    private String adjustParams;

    @Column(name = "trigger_rule", length = 100)
    @Comment("调整触发规则")
    private String triggerRule;

    @Column(name = "log_time", insertable = true, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("日志生成时间")
    @CreationTimestamp
    private LocalDateTime logTime;
}
