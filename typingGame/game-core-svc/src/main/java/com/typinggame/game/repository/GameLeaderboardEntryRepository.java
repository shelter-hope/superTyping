package com.typinggame.game.repository;

import com.typinggame.game.entity.LeaderboardEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameLeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, Integer>, JpaSpecificationExecutor<LeaderboardEntry> {
    List<LeaderboardEntry> findByUserIdAndGameMode(Integer userId, String gameMode);

    List<LeaderboardEntry> findByGameModeOrderByTotalScoreDesc(String gameMode, Pageable pageable);

    List<LeaderboardEntry> findByGameModeInOrderByTotalScoreDesc(List<String> gameModes, Pageable pageable);
}
