package com.typinggame.ai.repository;

import com.typinggame.entity.user.CharacterStats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterStatsRepository extends JpaRepository<CharacterStats, Integer> {
    List<CharacterStats> findByUserIdOrderByAverageAccuracyAsc(Integer userId, Pageable pageable);
}
