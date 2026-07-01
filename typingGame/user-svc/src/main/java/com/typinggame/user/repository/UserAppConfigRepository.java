package com.typinggame.user.repository;

import com.typinggame.entity.config.UserAppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAppConfigRepository extends JpaRepository<UserAppConfig, Integer> {

    Optional<UserAppConfig> findByUserId(Integer userId);
}