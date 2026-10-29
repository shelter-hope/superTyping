package com.typinggame.game.repository;

import com.typinggame.game.entity.StringParent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StringParentRepository extends JpaRepository<StringParent, Integer>, JpaSpecificationExecutor<StringParent> {
    Optional<StringParent> findByUserId(Integer userId);
}
