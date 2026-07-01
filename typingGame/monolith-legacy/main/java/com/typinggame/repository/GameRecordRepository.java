package com.typinggame.repository;

import com.typinggame.dto.leaderboard.LeaderboardDTO;
import com.typinggame.entity.game.GameRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;

/**
 * 游戏记录仓库
 */
@Repository
public interface GameRecordRepository extends JpaRepository<GameRecord, Integer> {
    List<GameRecord> findByUserId(Integer userId, Pageable pageable);
    List<GameRecord> findByUserIdAndGameMode(Integer userId, String gameMode, Pageable pageable);
    
    // Date range methods
    List<GameRecord> findByUserIdAndCreateTimeBetween(Integer userId, Date startDate, Date endDate, Pageable pageable);
    List<GameRecord> findByUserIdAndCreateTimeAfter(Integer userId, Date startDate, Pageable pageable);
    /** 指定用户、时间区间内的所有记录（用于统计聚合，不分页） */
    List<GameRecord> findByUserIdAndCreateTimeBetween(Integer userId, Date startDate, Date endDate);
    
    // Admin Pagination methods
    Page<GameRecord> findByGameMode(String gameMode, Pageable pageable);
    
    Page<GameRecord> findAllByUserId(Integer userId, Pageable pageable);
    Page<GameRecord> findAllByUserIdAndGameMode(Integer userId, String gameMode, Pageable pageable);
    
    GameRecord findTopByUserIdAndGameModeOrderByTotalScoreDesc(Integer userId, String gameMode);

    @Query("SELECT new com.typinggame.dto.leaderboard.LeaderboardDTO(" +
           "u.userId, u.nickname, u.avatar, g.totalScore, g.avgSpeed, g.avgAccuracy, g.createTime) " +
           "FROM GameRecord g, UserInfo u " +
           "WHERE g.userId = u.userId AND g.gameMode = :gameMode " +
           "ORDER BY g.totalScore DESC")
    List<LeaderboardDTO> findLeaderboardByMode(String gameMode, Pageable pageable);

    void deleteByUserId(Integer userId);
}
