package com.typinggame.game.repository;

import com.typinggame.entity.dictionaries.CustomDictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GameCustomDictionaryRepository extends JpaRepository<CustomDictionary, Integer> {
    List<CustomDictionary> findByDictIdInAndUserId(Collection<Integer> dictIds, Integer userId);
}
