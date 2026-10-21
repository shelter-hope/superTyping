package com.typinggame.game.repository;

import com.typinggame.entity.leaderboard.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameLeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, Integer> {
    List<LeaderboardEntry> findByUserIdAndGameMode(Integer userId, String gameMode);
}
