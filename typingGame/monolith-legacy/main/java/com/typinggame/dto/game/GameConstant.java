package com.typinggame.dto.game;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 游戏核心常量/辅助参数（全局统一配置）
 * 对应文档：基础参数-辅助参数/核心权重参数
 */
@Data
public class GameConstant {
    // 辅助参数（阈值/常量）
    private BigDecimal speedAdjustThresholdLow;  // 速度调节阈值A1=0.4
    private BigDecimal speedAdjustThresholdHigh; // 速度调节阈值A2=0.85
    private Integer countGrowThresholdLow;      // 数量增长阈值S1=40（字符/分钟）
    private Integer countGrowThresholdHigh;     // 数量增长阈值S2=120（字符/分钟）
    private BigDecimal lengthAdjustThresholdLow; // 长度调节阈值A3=0.5
    private BigDecimal lengthAdjustThresholdHigh;// 长度调节阈值A4=0.8

    private BigDecimal maxRefSpeed;             // 最大参考速度Smax=150（字符/分钟）
    private BigDecimal refPeakSpeed;            // 参考峰值速度Sref=120（字符/分钟）

    private BigDecimal initDifficulty;          // 初始难度系数D0=0.3
    private BigDecimal smoothAdjustCoeff;       // 平滑调节系数α=0.8

    // 核心权重参数（总和=1）
    private BigDecimal weightRealAccuracy;      // 实时准确率权重WA=0.35
    private BigDecimal weightRealSpeed;         // 实时速度权重WS=0.25
    private BigDecimal weightCharErrorRate;     // 单字符错误率权重WE=0.2
    private BigDecimal weightHistoryAccuracy;   // 历史平均准确率权重WAh=0.1
    private BigDecimal weightHistoryPeakSpeed;  // 历史峰值速度权重WSh=0.05
    private BigDecimal weightErrorCharRatio;    // 易错字符占比权重WRe=0.05
    private BigDecimal weightCurrentRound;      // 当前轮次权重WK=0.1

    // 当前游戏模式系数
    private BigDecimal ModeK0;  // 比如:模式难度增长系数K0=1.1
}