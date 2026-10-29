package com.typinggame.entity.leaderboard;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 排行榜条目实体 （对应leaderboard_entry表）
 */
@Entity
@Table(name = "leaderboard_entry",
        uniqueConstraints = @UniqueConstraint(name = "uk_leaderboard_user_mode", columnNames = {"user_id", "game_mode"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Integer entryId; // 条目ID

    @Column(name = "record_id")
    private Integer recordId; // 游戏记录ID

    @Column(name = "user_id")
    private Integer userId; // 用户ID

    @Column(name = "username")
    private String username; // 用户登录账号

    @Column(name = "nickname")
    private String nickname; // 昵称

    @Column(name = "avatar")
    private String avatar; // 玩家头像URL

    @Column(name = "game_mode")
    private String gameMode; // 游戏模式

    @Column(name = "score")
    private Integer totalScore; // 单场游戏总积分 (DB column is score, field is totalScore)

    @Column(name = "rounds")
    private Integer roundNum; // 游戏轮数 (DB column is rounds, field is roundNum)

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

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime; // 游戏记录创建时间 (DB column is created_at)

    @Column(name = "ranking")
    private Integer rank; // 排行榜名次

    // 辅助方法：格式化游戏时长为“X分X秒”
    public String getFormattedDuration() {
        if (gameDuration == null) return "0秒";
        int seconds = gameDuration / 1000;
        int minutes = seconds / 60;
        int remainSeconds = seconds % 60;
        return String.format("%d分%d秒", minutes, remainSeconds);
    }
}
