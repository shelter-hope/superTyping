package com.typinggame.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 打字行为分析结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingBehaviorDTO {
    /** 平均打字速度（字符/分钟） */
    private Double avgSpeed;
    /** 速度标准差（稳定性） */
    private Double speedStdDev;
    /** 平均准确率（%） */
    private Double avgAccuracy;
    /** 总游戏场次 */
    private Integer totalGames;
    /** 总输入字符数 */
    private Integer totalChars;
    /** 总错误数 */
    private Integer totalErrors;
    /** 按字符类型的错误分布：charType -> errorCount */
    private Map<String, Integer> errorDistributionByType;
    /** 易错字符 TopN：char -> errorCount */
    private List<CharErrorItem> topErrorChars;
    /** 最近 N 场平均速度（用于稳定性） */
    private List<Double> recentSpeeds;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharErrorItem {
        private String character;
        private Integer errorCount;
        private Double accuracy;
    }
}
