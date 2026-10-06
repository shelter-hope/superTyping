package com.typinggame.entity.game;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "game_round")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roundId;
    private Long sessionId;
    private Integer roundNum;
    private Long planVersion;
    private String formulaVersion;
    private BigDecimal difficultyBefore;
    private BigDecimal difficultyAfter;
    private BigDecimal targetAccuracy;
    private BigDecimal actualAccuracy;
    private BigDecimal avgSpeed;
    private BigDecimal monsterSpeed;
    private Integer monsterCount;
    private Integer stringLength;
    private Integer normalRatio;
    private Integer attackerRatio;
    private Integer evasiveRatio;
    private Integer smartRatio;
    private String planPayload;
    private String status;
    private Date createdAt;
    private Date completedAt;
}
