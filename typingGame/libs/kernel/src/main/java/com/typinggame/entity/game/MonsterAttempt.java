package com.typinggame.entity.game;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "monster_attempt", uniqueConstraints = @UniqueConstraint(name = "uk_monster_attempt_key", columnNames = {"round_id", "monster_key"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonsterAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attemptId;
    private Long sessionId;
    private Long roundId;
    private Long planVersion;
    private String monsterKey;
    private String targetStr;
    private String monsterType;
    private BigDecimal speed;
    private Integer inputCount;
    private Integer correctCount;
    private Integer errorCount;
    private String result;
    private Integer clientScore;
    private Integer verifiedScore;
    private BigDecimal fitness;
    private Integer generation;
    private Date createdAt;
    private Date eliminatedAt;
}
