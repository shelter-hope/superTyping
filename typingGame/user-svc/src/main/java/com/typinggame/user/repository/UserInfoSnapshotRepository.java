package com.typinggame.user.repository;

import com.typinggame.entity.user.UserInfoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoSnapshotRepository extends JpaRepository<UserInfoSnapshot, Integer>, JpaSpecificationExecutor<UserInfoSnapshot> {
}
