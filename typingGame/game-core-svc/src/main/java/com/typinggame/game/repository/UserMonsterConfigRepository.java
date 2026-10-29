package com.typinggame.game.repository;

import com.typinggame.game.entity.UserMonsterConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMonsterConfigRepository extends JpaRepository<UserMonsterConfig, Integer>, JpaSpecificationExecutor<UserMonsterConfig> {
}
