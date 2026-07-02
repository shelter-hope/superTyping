package com.typinggame.game.repository;

import com.typinggame.entity.leaderboard.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameLeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, Integer> {
    Optional<LeaderboardEntry> findByUserIdAndGameMode(Integer userId, String gameMode);
}
