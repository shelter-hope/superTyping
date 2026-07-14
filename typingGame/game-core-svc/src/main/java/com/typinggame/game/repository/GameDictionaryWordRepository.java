package com.typinggame.game.repository;

import com.typinggame.entity.dictionaries.DictionaryWord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GameDictionaryWordRepository extends JpaRepository<DictionaryWord, Integer> {
    List<DictionaryWord> findByDictIdIn(Collection<Integer> dictIds, Pageable pageable);

    List<DictionaryWord> findByDictIdInAndWordIn(Collection<Integer> dictIds, Collection<String> words);
}
