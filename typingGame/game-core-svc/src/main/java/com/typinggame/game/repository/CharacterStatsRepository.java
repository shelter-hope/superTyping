package com.typinggame.game.repository;

import com.typinggame.game.entity.CharacterStats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterStatsRepository extends JpaRepository<CharacterStats, Integer>, JpaSpecificationExecutor<CharacterStats> {
    Optional<CharacterStats> findByUserIdAndCharValue(Integer userId, String charValue);

    List<CharacterStats> findByUserId(Integer userId);

    List<CharacterStats> findByUserIdOrderByErrorCountDesc(Integer userId, Pageable pageable);

    List<CharacterStats> findByUserIdOrderByAverageAccuracyAsc(Integer userId, Pageable pageable);

    void deleteByUserId(Integer userId);
}
