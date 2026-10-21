package com.typinggame.entity.dictionaries;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 系统词库单词实体 （对应system_dictionary_words表）
 */
@Entity
@Table(name = "system_dictionary_words")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemDictionaryWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer wordId; // 单词ID（自增）
    private Integer dictId; // 关联词库ID
    private String word; // 单词
    private Integer total_count; // 该单词的总练习次数
    private Integer error_count; // 错误次数
    private Integer difficultyLevel; // 难度等级（1-5）
    private BigDecimal error_rate; // 该单词的总用户错误率（%）
    private Long user_avg_input_time; // 用户平均输入时长（毫秒）
    private BigDecimal frequency; // 使用频率
}