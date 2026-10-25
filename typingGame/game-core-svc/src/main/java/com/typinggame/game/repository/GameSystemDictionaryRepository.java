package com.typinggame.game.repository;

import com.typinggame.entity.dictionaries.SystemDictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 对局抽词直接读词库表。词库的增删改由 dict-svc 负责，这里不走 HTTP，避免把一次出词拆成跨服务调用。
 */
@Repository
public interface GameSystemDictionaryRepository extends JpaRepository<SystemDictionary, Integer> {
    List<SystemDictionary> findByDictIdInAndIsActiveTrue(Collection<Integer> dictIds);
}
