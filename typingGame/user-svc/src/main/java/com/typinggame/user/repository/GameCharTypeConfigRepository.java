package com.typinggame.user.repository;

import com.typinggame.entity.config.GameCharTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameCharTypeConfigRepository extends JpaRepository<GameCharTypeConfig, Integer> {
}