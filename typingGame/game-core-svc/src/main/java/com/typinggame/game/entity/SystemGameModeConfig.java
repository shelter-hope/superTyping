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
 * 游戏模式配置表实体 （对应game_mode_configs表）
 */
@Entity
@Table(name = "game_mode_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemGameModeConfig {
    @Id
    private Integer id; // 主键ID
    private String modeName; // 游戏模式
    private String description; // 模式描述
    private BigDecimal mode_k0; // 模式专属系数（难度增长系数）
    private Integer monster_init_num; // 初始化首轮怪物生成数量
    private String monster_type_pro; // 各种怪物的类型的占比，以json存储
    private Date createTime; // 创建时间
    private Integer createId; // 创建者的用户ID
    private Date updateTime; // 更新时间
    private Integer updateId; // 更新者的用户ID
}