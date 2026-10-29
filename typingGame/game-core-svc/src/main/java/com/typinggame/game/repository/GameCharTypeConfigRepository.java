package com.typinggame.game.repository;

import com.typinggame.game.entity.GameCharTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameCharTypeConfigRepository extends JpaRepository<GameCharTypeConfig, Integer> {
    Optional<GameCharTypeConfig> findByUserIdAndGameModeId(Integer userId, Integer gameModeId);
}
