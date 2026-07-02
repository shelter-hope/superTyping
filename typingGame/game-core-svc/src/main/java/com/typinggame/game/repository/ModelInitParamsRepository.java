package com.typinggame.game.repository;

import com.typinggame.entity.model.ModelInitParams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelInitParamsRepository extends JpaRepository<ModelInitParams, Integer> {
    List<ModelInitParams> findByGameMode(String gameMode);
}
