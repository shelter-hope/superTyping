package com.typinggame.repository;

import com.typinggame.entity.game.MonsterData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonsterDataRepository extends JpaRepository<MonsterData, Integer> {
    List<MonsterData> findByRecordId(Integer recordId);

    /**
     * 按用户ID删除其所有游戏记录关联的怪物明细数据
     */
    @Modifying
    @Query("DELETE FROM MonsterData m WHERE m.recordId IN (" +
            "SELECT g.recordId FROM GameRecord g WHERE g.userId = :userId)")
    void deleteByUserId(Integer userId);
}
