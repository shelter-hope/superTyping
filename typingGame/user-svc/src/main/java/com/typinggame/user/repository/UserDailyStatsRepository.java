package com.typinggame.user.repository;

import com.typinggame.entity.user.UserDailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDailyStatsRepository extends JpaRepository<UserDailyStats, Integer> {

    Optional<UserDailyStats> findByUserIdAndStatDate(Integer userId, LocalDate statDate);

    List<UserDailyStats> findByUserIdAndStatDateBetween(Integer userId, LocalDate startDate, LocalDate endDate);

    List<UserDailyStats> findByUserIdAndStatDateAfter(Integer userId, LocalDate startDate);

    List<UserDailyStats> findByUserIdAndStatDateBefore(Integer userId, LocalDate endDate);

    List<UserDailyStats> findByUserId(Integer userId);

    void deleteByUserId(Integer userId);
}