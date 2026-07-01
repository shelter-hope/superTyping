package com.typinggame.repository;

import com.typinggame.entity.game.UserGameModeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户游戏模式配置表数据访问层
 */
@Repository
public interface UserGameModeConfigRepository extends JpaRepository<UserGameModeConfig, Integer> {
    Optional<UserGameModeConfig> findByUser_UserIdAndModeName(Integer userId, String modeName);
}
