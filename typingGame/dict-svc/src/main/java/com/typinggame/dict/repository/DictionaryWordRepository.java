package com.typinggame.dict.repository;

import com.typinggame.dict.entity.DictionaryWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DictionaryWordRepository extends JpaRepository<DictionaryWord, Integer> {
    Page<DictionaryWord> findByDictId(Integer dictId, Pageable pageable);

    /** 内部接口：按词库批量取词（游戏侧只取前 N 条）。 */
    List<DictionaryWord> findByDictIdIn(Collection<Integer> dictIds, Pageable pageable);

    /** 内部接口：结算时按词定位，累计练习/错误次数。 */
    List<DictionaryWord> findByDictIdInAndWordIn(Collection<Integer> dictIds, Collection<String> words);

    void deleteByDictId(Integer dictId);
}
