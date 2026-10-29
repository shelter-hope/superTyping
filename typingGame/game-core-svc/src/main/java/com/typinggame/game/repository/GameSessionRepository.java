package com.typinggame.game.repository;

import com.typinggame.game.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    Optional<GameSession> findBySessionIdAndUserId(Long sessionId, Integer userId);
}
