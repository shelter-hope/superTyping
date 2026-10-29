package com.typinggame.dto.user;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日统计回写入参：一局结束后由 game-core 计算本局增量，交给 user-svc 累加。
 */
@Data
public class DailyStatsUpdateDTO {

    private LocalDate statDate;
    private String gameMode;
    private Integer totalScore;
    private Integer totalChars;
    private Integer correctChars;
    private BigDecimal avgSpeed;
    private BigDecimal avgAccuracy;
    private BigDecimal peakSpeed;
    private BigDecimal peakAccuracy;
}
