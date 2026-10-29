package com.typinggame.game.entity;

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
@Table(name = "model_init_params")
@NoArgsConstructor
@AllArgsConstructor
public class ModelInitParams {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "param_id", nullable = false)
    @Comment("主键ID")
    private Integer paramId;

    @Column(name = "game_mode", nullable = false, unique = true)
    @Comment("游戏模式")
    private String gameMode;

    @Column(name = "init_difficulty", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.30")
    @Comment("初始化系数")
    private BigDecimal initDifficulty = BigDecimal.valueOf(0.30);

    @Column(name = "weight_real_accuracy", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.35")
    @Comment("实时准确率权重WA")
    private BigDecimal weightRealAccuracy = BigDecimal.valueOf(0.35);

    @Column(name = "weight_real_speed", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.25")
    @Comment("实时速度权重WS")
    private BigDecimal weightRealSpeed = BigDecimal.valueOf(0.25);

    @Column(name = "weight_char_error_rate", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.20")
    @Comment("单字符错误率权重WE")
    private BigDecimal weightCharErrorRate = BigDecimal.valueOf(0.20);

    @Column(name = "weight_history_accuracy", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.10")
    @Comment("历史平均准确率权重WAh")
    private BigDecimal weightHistoryAccuracy = BigDecimal.valueOf(0.10);

    @Column(name = "weight_history_peak_speed", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.05")
    @Comment("历史峰值速度权重WSh")
    private BigDecimal weightHistoryPeakSpeed = BigDecimal.valueOf(0.05);

    @Column(name = "weight_error_char_ratio", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.05")
    @Comment("易错字符占比权重WRe")
    private BigDecimal weightErrorCharRatio = BigDecimal.valueOf(0.05);

    @Column(name = "weight_current_round", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.10")
    @Comment("轮次权重WK")
    private BigDecimal weightCurrentRound = BigDecimal.valueOf(0.10);

    @Column(name = "fatigue_buffer_flag", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Comment("疲劳缓冲标记")
    private Boolean fatigueBufferFlag = false;

    @Column(name = "progress_speed_flag", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Comment("进步速度标记")
    private Boolean progressSpeedFlag = false;

    @Column(name = "rollback_flag", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Comment("回退机制标记")
    private Boolean rollbackFlag = false;

    @Column(name = "max_adjust_ratio", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.30")
    @Comment("单次调节幅度上限（30%）")
    private BigDecimal maxAdjustRatio = BigDecimal.valueOf(0.30);

    @Column(name = "smooth_adjust_coeff", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.80")
    @Comment("平滑调节系数α")
    private BigDecimal smoothAdjustCoeff = BigDecimal.valueOf(0.80);

    @Column(name = "consecutive_mismatch_round", columnDefinition = "INT DEFAULT 0")
    @Comment("连续难度不匹配轮次")
    private Integer consecutiveMismatchRound = 0;

    @Column(name = "last_adjust_time")
    @Comment("最后一次参数调整时间")
    private LocalDateTime lastAdjustTime;

    @Column(name = "create_time", insertable = true, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("创建时间")
    @CreationTimestamp
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = true, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Comment("更新时间")
    @CreationTimestamp
    private LocalDateTime updateTime;
}