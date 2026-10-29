package com.typinggame.game.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 模型常量表实体 （对应model_constants表）
 */
@Entity
@Table(name = "model_constants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelConstant {
    @Id
    private Integer constantId; // 常量ID
    private BigDecimal maxSpeed; // 最大参考速度（字符/分钟）
    private BigDecimal refPeakSpeed; // 参考峰值速度（字符/分钟）
    private BigDecimal initialDifficulty; // 初始难度系数
    private Date updateTime; // 更新时间
}