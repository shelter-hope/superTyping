package com.typinggame.repository;

import com.typinggame.entity.user.CharacterStats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 字符统计仓库
 */
@Repository
public interface CharacterStatsRepository extends JpaRepository<CharacterStats, Integer> {
    Optional<CharacterStats> findByUserIdAndCharValue(Integer userId, String charValue);
    List<CharacterStats> findByUserId(Integer userId);
    List<CharacterStats> findByUserIdOrderByErrorCountDesc(Integer userId, Pageable pageable);
    List<CharacterStats> findByUserIdOrderByAverageAccuracyAsc(Integer userId, Pageable pageable);

    /**
     * 按用户ID删除其所有字符统计数据
     */
    void deleteByUserId(Integer userId);
}
