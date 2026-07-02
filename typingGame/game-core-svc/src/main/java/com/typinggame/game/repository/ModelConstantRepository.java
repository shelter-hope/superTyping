package com.typinggame.game.repository;

import com.typinggame.entity.model.ModelConstant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelConstantRepository extends JpaRepository<ModelConstant, Integer> {
}
