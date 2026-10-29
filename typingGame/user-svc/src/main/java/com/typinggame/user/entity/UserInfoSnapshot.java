package com.typinggame.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 玩家历史快照表实体 （对应user_info_snapshot表）
 */
@Entity
@Table(name = "user_info_snapshot")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoSnapshot {
    @Id
    private Integer id; // 主键ID
    private Integer userId; // 关联用户ID
    private Date snapshotTime; // 快照时间
    private Integer bestScore; // 快照时的历史最佳积分
    private Integer bestRound; // 快照时的历史最佳轮次
    private BigDecimal bestSpeed; // 快照时的历史最佳速度
    private BigDecimal bestAccuracy; // 快照时的历史最佳准确率
    private BigDecimal avgSpeed; // 快照时的平均速度
    private BigDecimal avgAccuracy; // 快照时的平均准确率
    private Integer gameNum; // 快照时的游戏总场数
}