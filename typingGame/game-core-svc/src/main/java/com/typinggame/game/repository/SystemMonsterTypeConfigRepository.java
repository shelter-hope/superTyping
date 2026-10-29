package com.typinggame.game.repository;

import com.typinggame.game.entity.SystemMonsterTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemMonsterTypeConfigRepository extends JpaRepository<SystemMonsterTypeConfig, Integer> {
    List<SystemMonsterTypeConfig> findByIsActiveTrue();
}
