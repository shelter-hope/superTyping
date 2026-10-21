package com.typinggame.lab.repository;

import com.typinggame.lab.entity.LabAlgorithm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabAlgorithmRepository extends JpaRepository<LabAlgorithm, String> {
    List<LabAlgorithm> findByEnabledTrueOrderBySortOrderAscIdAsc();

    List<LabAlgorithm> findAllByOrderBySortOrderAscIdAsc();
}
