package com.typinggame.game.repository;

import com.typinggame.entity.config.GameRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRuleConfigRepository extends JpaRepository<GameRuleConfig, Long> {
    List<GameRuleConfig> findByRuleKeyOrderByVersionDesc(String ruleKey);

    Optional<GameRuleConfig> findFirstByRuleKeyAndStatusOrderByVersionDesc(String ruleKey, String status);

    Optional<GameRuleConfig> findByRuleKeyAndVersion(String ruleKey, Integer version);

    List<GameRuleConfig> findByRuleKeyAndStatus(String ruleKey, String status);
}
