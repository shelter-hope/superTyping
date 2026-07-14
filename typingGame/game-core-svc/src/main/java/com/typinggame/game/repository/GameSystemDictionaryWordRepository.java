package com.typinggame.game.repository;

import com.typinggame.entity.dictionaries.SystemDictionaryWord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GameSystemDictionaryWordRepository extends JpaRepository<SystemDictionaryWord, Integer> {
    List<SystemDictionaryWord> findByDictIdIn(Collection<Integer> dictIds, Pageable pageable);
}
