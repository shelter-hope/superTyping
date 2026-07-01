package com.typinggame.repository;

import com.typinggame.entity.dictionaries.SystemDictionaryWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统词库单词仓库
 */
@Repository
public interface SystemDictionaryWordRepository extends JpaRepository<SystemDictionaryWord, Integer> {

    java.util.List<SystemDictionaryWord> findByDictId(Integer dictId);

    java.util.List<SystemDictionaryWord> findByDictIdIn(java.util.List<Integer> dictIds);
}