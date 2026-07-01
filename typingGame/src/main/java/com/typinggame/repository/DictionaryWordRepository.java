package com.typinggame.repository;

import com.typinggame.entity.dictionaries.DictionaryWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 自定义词库单词仓库
 */
@Repository
public interface DictionaryWordRepository extends JpaRepository<DictionaryWord, Integer> {

    java.util.List<DictionaryWord> findByDictId(Integer dictId);

    java.util.List<DictionaryWord> findByDictIdIn(java.util.List<Integer> dictIds);

    void deleteByDictId(Integer dictId);
}