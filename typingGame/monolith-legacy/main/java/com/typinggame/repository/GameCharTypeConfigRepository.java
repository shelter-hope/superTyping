package com.typinggame.repository;

import com.typinggame.entity.config.GameCharTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 游戏字符类型配置仓库
 */
@Repository
public interface GameCharTypeConfigRepository extends JpaRepository<GameCharTypeConfig, Integer> {
    Optional<GameCharTypeConfig> findByUserIdAndGameModeId(Integer userId, Integer gameModeId);
}