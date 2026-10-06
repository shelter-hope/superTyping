package com.typinggame.game.repository;

import com.typinggame.entity.game.MonsterAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonsterAttemptRepository extends JpaRepository<MonsterAttempt, Long> {
    List<MonsterAttempt> findByRoundId(Long roundId);
    List<MonsterAttempt> findBySessionIdAndResultOrderByFitnessDesc(Long sessionId, String result);
}
