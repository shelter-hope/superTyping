package com.typinggame.game.repository;

import com.typinggame.entity.game.UserMonsterConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMonsterConfigRepository extends JpaRepository<UserMonsterConfig, Integer> {
}
