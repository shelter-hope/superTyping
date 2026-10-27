package com.typinggame.lab.repository;

import com.typinggame.lab.entity.LabRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabRunRepository extends JpaRepository<LabRun, Long> {
    Page<LabRun> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    Page<LabRun> findByUserIdAndAlgorithmIdOrderByCreatedAtDesc(Integer userId, String algorithmId, Pageable pageable);

    Optional<LabRun> findByIdAndUserId(Long id, Integer userId);
}
