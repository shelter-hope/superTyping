package com.typinggame.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameRoundCompleteResultDTO {
    private Long roundId;
    private Integer roundNum;
    private BigDecimal verifiedAccuracy;
    private BigDecimal killRate;
    private Integer verifiedScore;
    private Integer attemptCount;
    private String status;
}
