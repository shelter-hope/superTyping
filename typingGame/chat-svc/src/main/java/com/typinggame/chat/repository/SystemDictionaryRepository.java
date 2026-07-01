package com.typinggame.chat.repository;

import com.typinggame.entity.dictionaries.SystemDictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemDictionaryRepository extends JpaRepository<SystemDictionary, Integer> {
    List<SystemDictionary> findByIsActiveTrue();
}