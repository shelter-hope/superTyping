package com.typinggame.game.repository;

import com.typinggame.entity.user.UserDailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface GameUserDailyStatsRepository extends JpaRepository<UserDailyStats, Integer> {
    Optional<UserDailyStats> findByUserIdAndStatDate(Integer userId, Date statDate);
}
