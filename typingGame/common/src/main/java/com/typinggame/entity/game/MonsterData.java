package com.typinggame.entity.game;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 怪兽数据实体 （对应monster_data表）
 */
@Entity
@Table(name = "monster_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonsterData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monster_id")
    private Integer monsterId; // 怪兽ID（自增）

    @Column(name = "record_id")
    private Integer recordId; // 关联游戏记录ID

    @Column(name = "monster_type")
    private String monsterType; // 怪兽类型（NORMAL/ATTACKER/EVASIVE/SMART）

    @Column(name = "target_str")
    private String targetStr; // 携带字符串

    @Column(name = "speed")
    private BigDecimal speed; // 行进速度（像素/帧）

    @Column(name = "survival_time")
    private Integer survivalTime; // 存活时间（毫秒）

    @Column(name = "destroy_cost_time")
    private Integer destroyCostTime; // 消灭耗时（毫秒，未消灭则为NULL）

    @Column(name = "total_count")
    private Integer totalCount; // 输入总词次数

    @Column(name = "input_error_count")
    private Integer inputErrorCount; // 输入错误次数

    @Column(name = "is_true_mean")
    private Integer isTrueMean; // 是否为有意义字符串（1=是/0=否）

    @Column(name = "score")
    private Integer score; // 消灭后积分

    @Column(name = "difficulty_level")
    private Integer difficultyLevel; // 难度等级（1-5）

    @Column(name = "spawn_wave")
    private Integer spawnWave; // 产生于第几轮

    @Column(name = "is_parent")
    private Boolean isParent; // 是否为下一轮父本（1=是/0=否）

    @Column(name = "round_id")
    private Long roundId; // 新版游戏轮次ID

    @Column(name = "input_count")
    private Integer inputCount; // 输入总次数

    @Column(name = "correct_count")
    private Integer correctCount; // 正确输入次数

    @Column(name = "error_count")
    private Integer errorCount; // 错误输入次数

    @Column(name = "result")
    private String result; // KILLED/MISSED/EVADED/COLLISION/TIMEOUT

    @Column(name = "generation")
    private Integer generation; // 遗传代数

    @Column(name = "plan_version")
    private Long planVersion; // RoundPlan版本
}
