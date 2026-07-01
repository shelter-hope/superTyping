package com.typinggame.entity.game;

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
 * 用户怪物基础信息表实体 （对应user_monster_configs表）
 */
@Entity
@Table(name = "user_monster_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMonsterConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 配置ID
    private Integer userId; // 关联用户ID
    private BigDecimal initSpeed; // 初始化行进速度（像素/帧）
    private Integer initStrLength; // 初始化字符长度
    private String type; // 怪兽类型
    private Date updatedAt; // 更新时间
}