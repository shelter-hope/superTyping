package com.typinggame.game.repository;

import com.typinggame.entity.game.MonsterData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonsterDataRepository extends JpaRepository<MonsterData, Integer> {

    List<MonsterData> findByRecordId(Integer recordId);

    void deleteByUserId(Integer userId);
}