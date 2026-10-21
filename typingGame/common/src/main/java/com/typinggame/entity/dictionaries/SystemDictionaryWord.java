package com.typinggame.entity.dictionaries;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    private Integer wordId; // 单词ID（自增）
    private Integer dictId; // 关联词库ID
    private String word; // 单词
    @Column(name = "total_count")
    private Integer totalCount; // 该单词的总练习次数
    @Column(name = "error_count")
    private Integer errorCount; // 错误次数
    private Integer difficultyLevel; // 难度等级（1-5）
    @Column(name = "error_rate")
    private BigDecimal errorRate; // 该单词的总用户错误率（%）
    @Column(name = "user_avg_input_time")
    private Long userAvgInputTime; // 用户平均输入时长（毫秒）
    private BigDecimal frequency; // 使用频率
}