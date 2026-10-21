package com.typinggame.lab.repository;

import com.typinggame.lab.entity.LabPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LabPresetRepository extends JpaRepository<LabPreset, Long> {
    @Query("""
            select p from LabPreset p
            where (:algorithmId is null or p.algorithmId = :algorithmId)
              and (p.userId is null or p.isPublic = true)
            order by p.id asc
            """)
    List<LabPreset> findVisible(@Param("algorithmId") String algorithmId);

    List<LabPreset> findByUserIdAndAlgorithmIdOrderByIdAsc(Integer userId, String algorithmId);

    List<LabPreset> findByUserIdOrderByIdAsc(Integer userId);

    List<LabPreset> findByUserIdIsNullOrderByIdAsc();

    boolean existsByUserIdIsNullAndAlgorithmIdAndName(String algorithmId, String name);
}
