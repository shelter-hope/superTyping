package com.typinggame.entity.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "game_rule_config")
@Data
public class GameRuleConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "rule_key", nullable = false, length = 64)
    private String ruleKey;
    @Column(nullable = false)
    private Integer version;
    @Column(nullable = false, columnDefinition = "json")
    private String payload;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "update_id")
    private Integer updateId;
    @Column(name = "update_time")
    private Date updateTime;
}
