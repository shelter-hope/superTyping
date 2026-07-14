package com.typinggame.game.repository;

import com.typinggame.entity.game.MonsterData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonsterDataRepository extends JpaRepository<MonsterData, Integer> {

    List<MonsterData> findByRecordId(Integer recordId);

    Page<MonsterData> findByRecordId(Integer recordId, Pageable pageable);

    List<MonsterData> findByMonsterIdIn(List<Integer> monsterIds);

    @Query("SELECT m FROM MonsterData m WHERE m.recordId IN (SELECT r.recordId FROM GameRecord r WHERE r.userId = :userId) ORDER BY m.score DESC, m.survivalTime DESC")
    List<MonsterData> findCandidateParentsByUserId(Integer userId, Pageable pageable);

    void deleteByRecordId(Integer recordId);

    @Modifying
    @Query("DELETE FROM MonsterData m WHERE m.recordId IN (SELECT r.recordId FROM GameRecord r WHERE r.userId = :userId)")
    void deleteByUserId(Integer userId);
}
