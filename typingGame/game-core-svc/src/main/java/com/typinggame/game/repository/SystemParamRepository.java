package com.typinggame.game.repository;

import com.typinggame.game.entity.SystemParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemParamRepository extends JpaRepository<SystemParam, Integer>, JpaSpecificationExecutor<SystemParam> {
    Optional<SystemParam> findByParamName(String paramName);
}
