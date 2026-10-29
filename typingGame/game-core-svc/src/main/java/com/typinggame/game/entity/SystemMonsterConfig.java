package com.typinggame.game.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

/**
 * 系统怪物基础信息配置表实体类
 */
@Data
@Entity
@Table(name = "system_monster_configs")
public class SystemMonsterConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "init_speed", nullable = false)
    private Double initSpeed;

    @Column(name = "init_str_length", nullable = false)
    private Integer initStrLength;

    @Column(name = "type", nullable = false, unique = true)
    private String type;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "description")
    private String description;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;


}
