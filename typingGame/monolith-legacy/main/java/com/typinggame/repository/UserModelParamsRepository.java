package com.typinggame.repository;

import com.typinggame.entity.model.UserModelParams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户模型参数仓库
 */
@Repository
public interface UserModelParamsRepository extends JpaRepository<UserModelParams, Integer> {
    Optional<UserModelParams> findByUserIdAndGameMode(Integer userId, String gameMode);
}