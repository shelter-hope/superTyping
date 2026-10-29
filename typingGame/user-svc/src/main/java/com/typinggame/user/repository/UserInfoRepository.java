package com.typinggame.user.repository;

import com.typinggame.user.entity.UserInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Integer>, JpaSpecificationExecutor<UserInfo> {

    Optional<UserInfo> findByUserId(Integer userId);

    Optional<UserInfo> findByUsername(String username);

    List<UserInfo> findByNicknameContaining(String keyword);

    Page<UserInfo> findByUsernameContainingOrNicknameContaining(String username, String nickname, Pageable pageable);

    /** 供集群内广播等场景拉取全部用户 ID。 */
    @Query("select u.userId from UserInfo u")
    List<Integer> findAllUserIds();
}