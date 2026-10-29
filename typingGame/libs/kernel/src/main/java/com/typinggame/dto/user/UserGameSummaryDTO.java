package com.typinggame.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 一局结束后的用户汇总回写（game-core 结算完调用）。
 * 最佳成绩与总场数由 user-svc 依据自身数据判定，避免跨服务读改写。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGameSummaryDTO {
    private Integer totalScore;
    private Integer roundNum;
    private BigDecimal avgSpeed;
    private BigDecimal avgAccuracy;
    private BigDecimal peakSpeed;
    private BigDecimal peakAccuracy;
}
