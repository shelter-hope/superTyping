package com.typinggame.chat.repository;

import com.typinggame.entity.leaderboard.LeaderboardEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, Integer> {
    List<LeaderboardEntry> findByGameModeOrderByTotalScoreDesc(String gameMode, Pageable pageable);
    List<LeaderboardEntry> findByGameModeInOrderByTotalScoreDesc(List<String> gameModes, Pageable pageable);
}