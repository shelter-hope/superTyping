package com.typinggame.user.entity;

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
 * 用户应用配置表实体 （对应user_app_configs表）
 */
@Entity
@Table(name = "user_app_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAppConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer configId; // 配置ID
    private Integer userId; // 关联用户ID
    private Boolean isMusic; // 是否启用音效
    private Boolean isSoundEffect; // 是否启用音效效果
    private Boolean showGameRecords; // 是否显示游戏记录
    private Boolean showRankings; // 是否显示排行榜
    private Boolean isSearchable; // 是否可被检索
    private Boolean darkMode; // 是否启用暗黑模式
    private Boolean notificationsEnabled; // 是否启用通知
    private Boolean autoSave; // 是否自动保存游戏进度
    private String language; // 语言设置
    private Integer fontSize; // 字体大小
    private String theme; // 主题设置
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间
}