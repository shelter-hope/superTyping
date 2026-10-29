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
import java.util.Date;

/**
 * 自定义词库单词实体 （对应dictionary_words表）
 */
@Entity
@Table(name="dictionary_words")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer wordId; // 单词ID（自增）
    private Integer dictId; // 关联词库ID
    private String word; // 单词
    private Integer totalPracticeCount; // 该单词的总练习次数
    private Integer errorPracticeCount; // 错误次数
    private Integer proficiencyLevel; // 玩家对该单词的熟练度等级（1-5）
    private Integer difficultyLevel; // 难度等级（1-5）
    private Boolean isFocusWord; // 是否为重点训练单词（1=是/0=否）
    private BigDecimal userErrorRate; // 该单词的用户错误率（%）
    private Long userAvgInputTime; // 用户平均输入时长（毫秒）
    private Integer userContinuousCorrect; // 连续正确输入次数（连胜次数）
    private Date lastPracticeTime; // 最后一次练习时间
    private BigDecimal frequency; // 使用频率
}