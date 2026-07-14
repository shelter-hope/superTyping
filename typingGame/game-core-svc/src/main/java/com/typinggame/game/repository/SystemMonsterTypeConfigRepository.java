package com.typinggame.game.repository;

import com.typinggame.entity.config.SystemMonsterTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemMonsterTypeConfigRepository extends JpaRepository<SystemMonsterTypeConfig, Integer> {
    List<SystemMonsterTypeConfig> findByIsActiveTrue();
}
