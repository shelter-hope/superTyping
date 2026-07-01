package com.typinggame.repository;

import com.typinggame.entity.game.UserMonsterConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户怪物配置仓库
 */
@Repository
public interface UserMonsterConfigRepository extends JpaRepository<UserMonsterConfig, Integer> {

    List<UserMonsterConfig> findByUserId(Integer userId);

    Optional<UserMonsterConfig> findByUserIdAndType(Integer userId, String type);
}