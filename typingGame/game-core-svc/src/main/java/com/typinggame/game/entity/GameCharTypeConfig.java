package com.typinggame.game.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 游戏字符类型配置表实体 （对应game_char_type_configs表）
 */
@Entity
@Table(name = "game_char_type_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameCharTypeConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 配置ID
    private Integer userId; // 关联用户ID
    private Integer gameModeId; // 关联游戏模式ID
    private String charType; // 勾选的字符类型，逗号隔开
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间
}