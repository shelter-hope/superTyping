package com.typinggame.repository;

import com.typinggame.entity.model.ModelConstant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 模型常量仓库
 */
@Repository
public interface ModelConstantRepository extends JpaRepository<ModelConstant, Integer> {
}