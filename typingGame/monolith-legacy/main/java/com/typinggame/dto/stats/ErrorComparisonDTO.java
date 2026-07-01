package com.typinggame.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 错误次数对比 DTO（本周 vs 上周）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorComparisonDTO {
    /** 本周总错误数 */
    private Integer thisWeekErrors;
    /** 上周总错误数 */
    private Integer lastWeekErrors;
    /** 变化量：正数表示本周错误增多 */
    private Integer change;
    /** 变化率（如 0.1 表示增加 10%） */
    private Double changeRate;
    /** 本周易错字符 TopN */
    private List<CharErrorItem> thisWeekTopErrors;
    /** 上周易错字符 TopN */
    private List<CharErrorItem> lastWeekTopErrors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharErrorItem {
        private String character;
        private Integer errorCount;
        private Double accuracy;
    }
}
