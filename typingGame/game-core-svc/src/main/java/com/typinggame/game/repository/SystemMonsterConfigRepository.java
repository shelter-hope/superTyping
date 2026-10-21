package com.typinggame.game.repository;

import com.typinggame.entity.game.SystemMonsterConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemMonsterConfigRepository extends JpaRepository<SystemMonsterConfig, Integer> {
    Optional<SystemMonsterConfig> findByType(String type);
}
