package com.typinggame.repository;

import com.typinggame.entity.game.StringParent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 字符串父本表数据访问层
 */
@Repository
public interface StringParentRepository extends JpaRepository<StringParent, Integer> {

    /** 按用户查询，按更新时间倒序，取最近一次父本 */
    List<StringParent> findByUserIdOrderByUpdateTimeDesc(Integer userId, org.springframework.data.domain.Pageable pageable);
}
