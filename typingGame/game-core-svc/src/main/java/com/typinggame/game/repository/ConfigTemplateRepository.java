package com.typinggame.game.repository;

import com.typinggame.entity.config.ConfigTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigTemplateRepository extends JpaRepository<ConfigTemplate, Integer> {
    Optional<ConfigTemplate> findByName(String name);
}
