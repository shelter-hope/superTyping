package com.typinggame.repository;

import com.typinggame.entity.model.UserModelAdaptLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户模型适配日志表数据访问层
 */
@Repository
public interface UserModelAdaptLogRepository extends JpaRepository<UserModelAdaptLog, Integer> {

    List<UserModelAdaptLog> findByUserUserIdOrderByLogTimeDesc(Integer userId, Pageable pageable);

    Page<UserModelAdaptLog> findByUser_UserIdOrderByLogTimeDesc(Integer userId, Pageable pageable);

    Page<UserModelAdaptLog> findByUser_UserIdAndGameRecord_GameModeOrderByLogTimeDesc(
            Integer userId, String gameMode, Pageable pageable);

    Page<UserModelAdaptLog> findByUser_UserIdAndLogTimeBetweenOrderByLogTimeDesc(
            Integer userId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    /** 用于趋势聚合：按用户与时间范围查询（不分页） */
    List<UserModelAdaptLog> findByUser_UserIdAndLogTimeBetweenOrderByLogTimeDesc(
            Integer userId, LocalDateTime start, LocalDateTime end);

    /** 用于趋势聚合：按用户、模式与时间范围查询（不分页） */
    List<UserModelAdaptLog> findByUser_UserIdAndGameRecord_GameModeAndLogTimeBetweenOrderByLogTimeDesc(
            Integer userId, String gameMode, LocalDateTime start, LocalDateTime end);
}
