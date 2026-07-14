package com.typinggame.entity.user;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 玩家每日汇总表实体 （对应user_daily_stats表）
 */
@Entity
@Table(name = "user_daily_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDailyStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 主键ID
    private Integer userId; // 关联用户ID
    private Date statDate; // 统计日期（yyyy-MM-dd）
    private Integer gameCount; // 当日游戏场数
    private Integer totalScore; // 当日总积分
    private BigDecimal avgSpeed; // 当日平均速度（字符/分钟）
    private BigDecimal avgAccuracy; // 当日平均准确率（%）
    private BigDecimal peakSpeed; // 当日峰值速度
    private BigDecimal peakAccuracy; // 当日峰值准确率
    private Integer totalChars; // 当日总输入字符数
    private Integer correctChars; // 当日正确输入字符数
    private String gameMode; // 游戏模式
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间
}
