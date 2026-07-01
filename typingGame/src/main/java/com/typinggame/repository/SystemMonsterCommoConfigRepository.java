package com.typinggame.repository;

import com.typinggame.entity.config.SystemMonsterCommoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统怪物公共基础信息配置表数据访问层
 */
@Repository
public interface SystemMonsterCommoConfigRepository extends JpaRepository<SystemMonsterCommoConfig, Integer> {
}
