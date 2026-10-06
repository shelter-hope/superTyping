package com.typinggame.dto.game;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MonsterDataDTO {
    private String monsterType;
    private String targetStr;
    private BigDecimal speed;
    private Integer survivalTime;
    private Integer destroyCostTime;
    private Integer totalCount;
    private Integer inputErrorCount;
    private Integer isTrueMean;
    private Integer score;
    private Integer difficultyLevel;
    private Integer spawnWave;
    private Boolean isParent;
    private Long roundId;
    private Integer inputCount;
    private Integer correctCount;
    private Integer errorCount;
    private String result;
    private Integer generation;
    private Long planVersion;
}
