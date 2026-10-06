package com.typinggame.game.repository;

import com.typinggame.entity.game.GameRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRoundRepository extends JpaRepository<GameRound, Long> {
    Optional<GameRound> findBySessionIdAndRoundNum(Long sessionId, Integer roundNum);
}
