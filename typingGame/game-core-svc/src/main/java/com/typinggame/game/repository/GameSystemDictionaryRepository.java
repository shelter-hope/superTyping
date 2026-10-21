package com.typinggame.game.repository;

import com.typinggame.entity.dictionaries.SystemDictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GameSystemDictionaryRepository extends JpaRepository<SystemDictionary, Integer> {
    List<SystemDictionary> findByDictIdInAndIsActiveTrue(Collection<Integer> dictIds);
}
