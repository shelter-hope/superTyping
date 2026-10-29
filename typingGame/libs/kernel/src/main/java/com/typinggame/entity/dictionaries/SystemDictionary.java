package com.typinggame.entity.dictionaries;

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
 * 系统词库实体 （对应system_dictionaries表）
 */
@Entity
@Table(name = "system_dictionaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemDictionary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer dictId; // 词库ID（自增）
    private String name; // 词库名称
    private String description; // 词库描述
    private Integer wordCount; // 单词数量
    private String category; // 词库分类
    private Boolean isActive; // 是否启用（1=启用/0=禁用）
    private String fileUrl; // 词库文件URL
    private String fileName; // 词库文件名
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间
}