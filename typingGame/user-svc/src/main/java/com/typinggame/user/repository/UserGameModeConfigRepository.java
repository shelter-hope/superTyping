package com.typinggame.user.repository;

import com.typinggame.entity.game.UserGameModeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGameModeConfigRepository extends JpaRepository<UserGameModeConfig, Integer>, JpaSpecificationExecutor<UserGameModeConfig> {
}