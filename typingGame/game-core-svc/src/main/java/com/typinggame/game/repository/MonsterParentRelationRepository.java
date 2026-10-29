package com.typinggame.game.repository;

import com.typinggame.game.entity.MonsterParentRelation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonsterParentRelationRepository extends JpaRepository<MonsterParentRelation, Long> {
}
