package com.typinggame.lab.repository;

import com.typinggame.lab.entity.LabRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LabRunRepository extends JpaRepository<LabRun, Long> {
    Page<LabRun> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    Page<LabRun> findByUserIdAndAlgorithmIdOrderByCreatedAtDesc(Integer userId, String algorithmId, Pageable pageable);

    Optional<LabRun> findByIdAndUserId(Long id, Integer userId);

    @Query("""
            SELECT r FROM LabRun r
            WHERE (:userId IS NULL OR r.userId = :userId)
              AND (:algorithmId IS NULL OR r.algorithmId = :algorithmId)
              AND (:fromTime IS NULL OR r.createdAt >= :fromTime)
              AND (:toTime IS NULL OR r.createdAt <= :toTime)
            ORDER BY r.createdAt DESC
            """)
    Page<LabRun> adminSearch(
            @Param("userId") Integer userId,
            @Param("algorithmId") String algorithmId,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable);
}
