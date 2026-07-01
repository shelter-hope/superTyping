package com.typinggame.repository;

import com.typinggame.entity.config.SystemParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统参数仓库
 */
@Repository
public interface SystemParamRepository extends JpaRepository<SystemParam, Integer> {
}