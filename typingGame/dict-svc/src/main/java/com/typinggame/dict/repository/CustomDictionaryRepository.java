package com.typinggame.dict.repository;

import com.typinggame.dict.entity.CustomDictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CustomDictionaryRepository extends JpaRepository<CustomDictionary, Integer>, JpaSpecificationExecutor<CustomDictionary> {
    List<CustomDictionary> findByUserId(Integer userId);

    /** 内部接口：只返回属于该用户的词典（越权词典直接忽略）。 */
    List<CustomDictionary> findByDictIdInAndUserId(Collection<Integer> dictIds, Integer userId);
}