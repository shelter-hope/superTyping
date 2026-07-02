package com.typinggame.game.repository;

import com.typinggame.entity.game.StringParent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StringParentRepository extends JpaRepository<StringParent, Integer> {
}
