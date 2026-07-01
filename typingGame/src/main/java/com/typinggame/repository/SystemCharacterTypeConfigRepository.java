package com.typinggame.repository;

import com.typinggame.entity.config.SystemCharacterTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 字符类型配置仓库
 */
@Repository
public interface SystemCharacterTypeConfigRepository extends JpaRepository<SystemCharacterTypeConfig, Integer> {
}