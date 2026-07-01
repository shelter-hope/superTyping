package com.typinggame.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 下一局游戏推荐配置（基于用户统计与进化建议）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameRecommendDTO {
    /** 建议重点练习的字符列表（易错字符） */
    private List<String> focusChars;

    /** 推荐基础速度（像素/帧或字符/分钟，由前端解释） */
    private Double suggestedBaseSpeed;

    /** 推荐游戏模式（前端枚举值，如 endless / dictionary） */
    private String suggestedMode;

    /** 推荐生成的下一轮单词预览（可用于词源界定或展示） */
    private List<String> previewWords;

    /** 文字建议摘要 */
    private String summary;

    // === 以下字段来自数学模型调节（GameAdjustParam 映射） ===

    /** 推荐怪物数量 N_t */
    private Integer suggestedMonsterCount;

    /** 推荐平均字符串长度 L_t */
    private Integer suggestedAvgStringLength;

    /** 怪物类型占比（总和约为 100） */
    private Integer suggestedNormalRatio;
    private Integer suggestedAttackRatio;
    private Integer suggestedEvasiveRatio;
    private Integer suggestedSmartRatio;

    /** 当前轮综合难度系数 D_t（0-1 或更高，前端仅展示趋势） */
    private BigDecimal currentDifficulty;
}
