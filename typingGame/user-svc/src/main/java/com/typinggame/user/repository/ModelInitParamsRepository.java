package com.typinggame.user.repository;

import com.typinggame.entity.model.ModelInitParams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelInitParamsRepository extends JpaRepository<ModelInitParams, Integer> {
}