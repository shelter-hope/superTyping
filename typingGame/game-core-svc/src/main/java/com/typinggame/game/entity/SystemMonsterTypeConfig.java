package com.typinggame.game.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 各类型怪物初始化基本配置
 */
@Data
@Entity
@Table(name = "system_monster_type_configs")
@NoArgsConstructor
@AllArgsConstructor
public class SystemMonsterTypeConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Comment("配置ID")
    private Integer id;

    @Column(name = "init_speed", nullable = false, precision = 3, scale = 2)
    @Comment("初始化行进速度（像素/帧）")
    private BigDecimal initSpeed;

    @Column(name = "init_str_length", nullable = false)
    @Comment("初始化字符长度")
    private Integer initStrLength;

    @Column(name = "type", nullable = false, unique = true)
    @Comment("怪兽类型")
    private String type;

    @Column(name = "score", nullable = false)
    @Comment("积分值")
    private Integer score;

    @Column(name = "description", length = 200)
    @Comment("效果描述")
    private String description;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Comment("是否启用")
    private Boolean isActive = true;

    @Column(name = "updated_at", insertable = false, updatable = true, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Comment("更新时间")
    @CreationTimestamp
    private LocalDateTime updatedAt;
}