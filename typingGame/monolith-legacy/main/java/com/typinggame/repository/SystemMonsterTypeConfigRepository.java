package com.typinggame.repository;

import com.typinggame.entity.config.SystemMonsterTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统怪物类型基础信息配置表数据访问层
 */
@Repository
public interface SystemMonsterTypeConfigRepository extends JpaRepository<SystemMonsterTypeConfig, Integer> {
}
