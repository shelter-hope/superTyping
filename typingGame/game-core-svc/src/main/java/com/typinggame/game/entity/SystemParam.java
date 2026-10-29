package com.typinggame.game.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 系统参数表实体 （对应system_params表）
 */
@Entity
@Table(name = "system_params")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemParam {
    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Integer paramId; // 参数ID
    private String paramName; // 参数名称
    private String paramValue; // 参数值
    private String paramType; // 参数类型
    private String description; // 参数描述
    private Boolean isSystem; // 是否系统参数（1=是/0=否）
    private Boolean isActive; // 是否启用（1=启用/0=禁用）
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间
}