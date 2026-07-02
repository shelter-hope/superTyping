package com.typinggame.game.repository;

import com.typinggame.entity.config.SystemMonsterCommoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemMonsterCommoConfigRepository extends JpaRepository<SystemMonsterCommoConfig, Integer> {
}
