package com.typinggame.game.repository;

import com.typinggame.entity.user.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameUserInfoRepository extends JpaRepository<UserInfo, Integer> {
}
