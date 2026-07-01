package com.typinggame.dto.game;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 玩家实时+历史表现指标（输入变量）
 * 对应文档：基础参数-玩家表现核心指标
 */
@Data
public class GamePlayerPerformanceDTO {
    // 实时表现指标
    private BigDecimal realAccuracy; // 实时准确率At（0-1）
    private BigDecimal realSpeed;    // 实时打字速度St（字符/分钟）
    private BigDecimal avgCharErrorRate; // 单字符平均错误率Eit（0-1）

    // 历史表现指标
    private BigDecimal historyAvgAccuracy; // 历史平均准确率Ah（近5场均值）
    private BigDecimal historyPeakSpeed;   // 历史峰值速度Shmax（字符/分钟）
    private BigDecimal errorCharRatio;     // 易错字符占比Re（0-1）

    // 游戏进程指标
    private Integer currentRound; // 当前轮次K（首轮为1）
}
