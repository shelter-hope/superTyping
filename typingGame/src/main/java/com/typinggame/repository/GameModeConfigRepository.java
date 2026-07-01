package com.typinggame.repository;

import com.typinggame.entity.config.SystemGameModeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 游戏模式配置仓库
 */
@Repository
public interface GameModeConfigRepository extends JpaRepository<SystemGameModeConfig, Integer> {

    Optional<SystemGameModeConfig> findByModeName(String modeName);
}