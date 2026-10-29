package com.typinggame.game.repository;

import com.typinggame.game.entity.KeyConfusion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KeyConfusionRepository extends JpaRepository<KeyConfusion, Long> {
    Optional<KeyConfusion> findByUserIdAndExpectedCharAndTypedChar(Integer userId, String expectedChar, String typedChar);

    List<KeyConfusion> findByUserIdOrderByConfusionCountDesc(Integer userId, Pageable pageable);

    long countByUserId(Integer userId);
}
