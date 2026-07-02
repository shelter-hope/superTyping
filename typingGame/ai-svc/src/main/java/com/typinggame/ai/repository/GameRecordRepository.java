package com.typinggame.ai.repository;

import com.typinggame.entity.game.GameRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface GameRecordRepository extends JpaRepository<GameRecord, Integer> {
    List<GameRecord> findByUserId(Integer userId, Pageable pageable);

    List<GameRecord> findByUserIdAndCreateTimeBetween(Integer userId, Date startDate, Date endDate);
}
