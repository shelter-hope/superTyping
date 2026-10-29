package com.typinggame.game.repository;

import com.typinggame.game.entity.SystemMonsterCommoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemMonsterCommoConfigRepository extends JpaRepository<SystemMonsterCommoConfig, Integer> {
}
