package com.typinggame.entity.game;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 游戏记录实体 （对应game_record表）
 */
@Entity
@Table(name = "game_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Integer recordId; // 记录ID（自增）

    @Column(name = "user_id")
    private Integer userId; // 关联用户ID

    @Column(name = "game_mode")
    private String gameMode; // 游戏模式（endless/dictionary）

    @Column(name = "selected_chars")
    private String selectedChars; // 选择的字符类型（逗号分隔，如lower,number,punct）

    @Column(name = "round_num")
    private Integer roundNum; // 游戏轮数

    @Column(name = "total_score")
    private Integer totalScore; // 单轮总积分

    @Column(name = "total_chars")
    private Integer totalChars; // 总输入字符数

    @Column(name = "correct_chars")
    private Integer correctChars; // 正确输入字符数

    @Column(name = "avg_speed")
    private BigDecimal avgSpeed; // 平均打字速度（字符/分钟）

    @Column(name = "peak_speed")
    private BigDecimal peakSpeed; // 峰值打字速度（字符/分钟）

    @Column(name = "avg_accuracy")
    private BigDecimal avgAccuracy; // 平均准确率（%）

    @Column(name = "peak_accuracy")
    private BigDecimal peakAccuracy; // 峰值准确率（%）

    @Column(name = "game_duration")
    private Integer gameDuration; // 游戏时长（毫秒）

    @Column(name = "create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime; // 记录时间
}
