package com.typinggame.user.repository;

import com.typinggame.entity.model.UserModelParams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserModelParamsRepository extends JpaRepository<UserModelParams, Integer>, JpaSpecificationExecutor<UserModelParams> {

    Optional<UserModelParams> findByUserIdAndGameMode(Integer userId, String gameMode);
}