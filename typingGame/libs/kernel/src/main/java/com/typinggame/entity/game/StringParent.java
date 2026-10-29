package com.typinggame.entity.game;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 字符串父本实体 （对应string_parents表）
 */
@Entity
@Table(name = "string_parents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StringParent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 自增主键
    private Integer userId; // 关联用户ID
    private String strParents; // 父本字符串 （关联MonsterData的Id，逗号分隔）
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}