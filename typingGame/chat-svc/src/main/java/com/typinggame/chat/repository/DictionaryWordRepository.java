package com.typinggame.chat.repository;

import com.typinggame.entity.dictionaries.DictionaryWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DictionaryWordRepository extends JpaRepository<DictionaryWord, Integer> {
    Page<DictionaryWord> findByDictId(Integer dictId, Pageable pageable);

    void deleteByDictId(Integer dictId);
}
