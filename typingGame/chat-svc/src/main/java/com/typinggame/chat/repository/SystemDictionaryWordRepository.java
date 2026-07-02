package com.typinggame.chat.repository;

import com.typinggame.entity.dictionaries.SystemDictionaryWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemDictionaryWordRepository extends JpaRepository<SystemDictionaryWord, Integer> {
    Page<SystemDictionaryWord> findByDictId(Integer dictId, Pageable pageable);
}
