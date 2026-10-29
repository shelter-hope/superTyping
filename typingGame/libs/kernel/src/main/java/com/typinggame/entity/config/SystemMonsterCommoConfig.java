package com.typinggame.entity.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 怪物属性变化的约束值配置
 */
@Data
@Entity
@Table(name = "system_monster_commo_configs")
@NoArgsConstructor
@AllArgsConstructor
public class SystemMonsterCommoConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Comment("配置ID")
    private Integer id;

    @Column(name = "min_speed", nullable = false, precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.8")
    @Comment("最小行进速度")
    private BigDecimal minSpeed;

    @Column(name = "max_speed", nullable = false, precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 3.0")
    @Comment("最大行进速度")
    private BigDecimal maxSpeed;

    @Column(name = "min_str_length", nullable = false, columnDefinition = "INT DEFAULT 1")
    @Comment("最小字符长度")
    private Integer minStrLength;

    @Column(name = "max_str_length", nullable = false, columnDefinition = "INT DEFAULT 15")
    @Comment("最大字符长度")
    private Integer maxStrLength;

    @Column(name = "updated_at", insertable = false, updatable = true, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Comment("更新时间")
    @CreationTimestamp
    private LocalDateTime updatedAt;
}