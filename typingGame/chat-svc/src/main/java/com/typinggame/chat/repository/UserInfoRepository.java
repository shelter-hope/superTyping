package com.typinggame.chat.repository;

import com.typinggame.entity.user.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Integer> {
    Optional<UserInfo> findByUserId(Integer userId);

    @Query("SELECT u.userId FROM UserInfo u")
    List<Integer> findAllUserIds();
}
