package com.typinggame.repository;

import com.typinggame.entity.config.UserAppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户应用配置仓库
 */
@Repository
public interface UserAppConfigRepository extends JpaRepository<UserAppConfig, Integer> {
}