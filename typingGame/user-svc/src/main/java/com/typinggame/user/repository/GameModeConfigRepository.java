package com.typinggame.user.repository;

import com.typinggame.entity.config.SystemGameModeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameModeConfigRepository extends JpaRepository<SystemGameModeConfig, Integer> {
}