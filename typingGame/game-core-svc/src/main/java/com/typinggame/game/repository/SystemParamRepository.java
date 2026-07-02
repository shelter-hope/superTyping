package com.typinggame.game.repository;

import com.typinggame.entity.config.SystemParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemParamRepository extends JpaRepository<SystemParam, Integer> {
}
