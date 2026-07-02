package com.typinggame.game.repository;

import com.typinggame.entity.model.UserModelAdaptLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserModelAdaptLogRepository extends JpaRepository<UserModelAdaptLog, Integer> {
    List<UserModelAdaptLog> findByLogTimeGreaterThanEqualAndLogTimeLessThan(LocalDateTime start, LocalDateTime end);
}
