package com.typinggame.game.repository;

import com.typinggame.entity.game.StringParent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StringParentRepository extends JpaRepository<StringParent, Integer> {
    Optional<StringParent> findByUserId(Integer userId);
}
