package com.typinggame.game.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 字符统计实体 （对应character_stats表）
 */
@Entity
@Table(name = "character_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharacterStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer statId; // 统计ID（自增）
    private Integer userId; // 关联用户ID
    @Column(name = "char_value", nullable = false, length = 10, columnDefinition = "VARCHAR(10) NOT NULL COLLATE utf8mb4_bin")
    private String charValue; // 字符（单个字符，区分大小写，与 migrate_character_stats_case_sensitive 一致）
    private String charType; // 字符类型（lower/upper/number/punct/chinese/special）
    private Integer totalAttempts; // 总尝试次数
    private Integer correctAttempts; // 正确次数
    private Integer errorCount; // 总错误次数
    private BigDecimal averageAccuracy; // 平均准确率（%）
    private BigDecimal recentAccuracy; // 最近10次准确率（%）
    private Date updateTime; // 最后更新时间
}
