package com.typinggame.repository;

import com.typinggame.entity.dictionaries.CustomDictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 自定义词库仓库
 */
@Repository
public interface CustomDictionaryRepository extends JpaRepository<CustomDictionary, Integer> {
    List<CustomDictionary> findByUserId(Integer userId);
    List<CustomDictionary> findByIsPublicTrue();
}
