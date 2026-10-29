package com.typinggame.dict.repository;

import com.typinggame.dict.entity.SystemDictionaryWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SystemDictionaryWordRepository extends JpaRepository<SystemDictionaryWord, Integer> {
    Page<SystemDictionaryWord> findByDictId(Integer dictId, Pageable pageable);

    /** 内部接口：按词库批量取词（游戏侧只取前 N 条）。 */
    List<SystemDictionaryWord> findByDictIdIn(Collection<Integer> dictIds, Pageable pageable);

    int countByDictId(Integer dictId);

    void deleteByDictId(Integer dictId);
}
