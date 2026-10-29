package com.typinggame.dict.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 自定义词库实体 （对应custom_dictionaries表）
 */
@Entity
@Table(name ="custom_dictionaries" )
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomDictionary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer dictId; // 词库ID（自增）
    private Integer userId; // 关联用户ID
    private String name; // 词库名称
    @Column(length = 500)
    private String description; // 词库描述
    private Integer wordCount; // 单词数量
    private Boolean isPublic; // 是否公开（1=公开/0=私有）
    private String fileUrl; // 词库文件URL
    private String fileName; // 词库文件名
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间
}
