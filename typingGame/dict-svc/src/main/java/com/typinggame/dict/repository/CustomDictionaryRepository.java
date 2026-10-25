package com.typinggame.dict.repository;

import com.typinggame.entity.dictionaries.CustomDictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomDictionaryRepository extends JpaRepository<CustomDictionary, Integer>, JpaSpecificationExecutor<CustomDictionary> {
    List<CustomDictionary> findByUserId(Integer userId);
}