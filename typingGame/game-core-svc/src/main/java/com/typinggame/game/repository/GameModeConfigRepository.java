package com.typinggame.game.repository;

import com.typinggame.entity.config.SystemGameModeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameModeConfigRepository extends JpaRepository<SystemGameModeConfig, Integer> {
    Optional<SystemGameModeConfig> findByModeName(String modeName);
}
