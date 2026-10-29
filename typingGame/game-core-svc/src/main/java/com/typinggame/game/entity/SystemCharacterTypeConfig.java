package com.typinggame.game.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 字符类型配置表实体 （对应character_type_configs表）
 */
@Entity
@Table(name = "character_type_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemCharacterTypeConfig {
    @Id
    private Integer id; // 字符类型ID
    private String charType; // 字符类型
    private String description; // 类型的描述
    private String regexPattern; // 正则匹配式
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间
}