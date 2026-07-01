package com.typinggame.repository;

import com.typinggame.entity.model.ModelInitParams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统数学模型初始化参数表数据访问层
 */
@Repository
public interface ModelInitParamsRepository extends JpaRepository<ModelInitParams, Integer> {

    java.util.Optional<ModelInitParams> findFirstByGameMode(String gameMode);
}
