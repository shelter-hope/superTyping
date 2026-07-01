package com.typinggame.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 趋势图上的一个数据点（按日/周/月聚合）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointDTO {
    /** 日期或周期标签，如 "2025-03-01" / "第1周" / "2025-03" */
    private String label;
    /** 平均速度（字符/分钟） */
    private Double avgSpeed;
    /** 峰值速度 */
    private Double peakSpeed;
    /** 平均准确率（%） */
    private Double avgAccuracy;
    /** 峰值准确率（%） */
    private Double peakAccuracy;
    /** 该周期内游戏场次 */
    private Integer gameCount;
    /** 该周期内总错误数 */
    private Integer errorCount;
}
