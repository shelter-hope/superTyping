package com.typinggame.game.repository;

import com.typinggame.game.entity.GameRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface GameRecordRepository extends JpaRepository<GameRecord, Integer>, JpaSpecificationExecutor<GameRecord> {
    List<GameRecord> findByUserId(Integer userId, Pageable pageable);
    List<GameRecord> findByUserIdAndGameMode(Integer userId, String gameMode, Pageable pageable);

    List<GameRecord> findByUserIdAndCreateTimeBetween(Integer userId, Date startDate, Date endDate, Pageable pageable);
    List<GameRecord> findByUserIdAndCreateTimeAfter(Integer userId, Date startDate, Pageable pageable);
    List<GameRecord> findByUserIdAndCreateTimeBetween(Integer userId, Date startDate, Date endDate);

    Page<GameRecord> findByGameMode(String gameMode, Pageable pageable);

    Page<GameRecord> findAllByUserId(Integer userId, Pageable pageable);
    Page<GameRecord> findAllByUserIdAndGameMode(Integer userId, String gameMode, Pageable pageable);

    GameRecord findTopByUserIdAndGameModeOrderByTotalScoreDesc(Integer userId, String gameMode);

    void deleteByUserId(Integer userId);

    @Query("SELECT COUNT(DISTINCT g.userId) FROM GameRecord g WHERE g.createTime >= :since")
    long countDistinctUsersSince(@Param("since") Date since);
}