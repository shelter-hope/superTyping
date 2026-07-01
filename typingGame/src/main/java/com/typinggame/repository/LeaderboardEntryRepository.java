package com.typinggame.repository;

import com.typinggame.entity.leaderboard.LeaderboardEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 排行榜条目仓库
 */
@Repository
public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, Integer> {
    List<LeaderboardEntry> findByGameModeOrderByTotalScoreDesc(String gameMode, Pageable pageable);

    /** 按多种 game_mode 值查询（用于兼容历史 client 名与当前枚举名） */
    List<LeaderboardEntry> findByGameModeInOrderByTotalScoreDesc(List<String> gameModes, Pageable pageable);

    /**
     * 按用户ID删除其所有排行榜记录
     */
    void deleteByUserId(Integer userId);
}