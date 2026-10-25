package com.typinggame.dict.repository;

import com.typinggame.entity.dictionaries.SystemDictionary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemDictionaryRepository extends JpaRepository<SystemDictionary, Integer>, JpaSpecificationExecutor<SystemDictionary> {
    List<SystemDictionary> findByIsActiveTrue();

    @Query("""
            select d from SystemDictionary d
            where (:name is null or lower(d.name) like lower(concat('%', :name, '%')))
              and (:category is null or lower(d.category) like lower(concat('%', :category, '%')))
            """)
    Page<SystemDictionary> search(@Param("name") String name, @Param("category") String category, Pageable pageable);
}