package com.typinggame.repository;

import com.typinggame.entity.user.UserInfoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户信息快照仓库
 */
@Repository
public interface UserInfoSnapshotRepository extends JpaRepository<UserInfoSnapshot, Integer> {
}