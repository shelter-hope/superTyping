package com.typinggame.user.repository;

import com.typinggame.entity.message.FriendRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRelationRepository extends JpaRepository<FriendRelation, Integer> {

    List<FriendRelation> findByUserIdOrFriendUserIdAndRelationStatus(Integer userId, Integer friendUserId, Integer relationStatus);

    Optional<FriendRelation> findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(Integer userId1, Integer userId2, Integer userId3, Integer userId4);

    List<FriendRelation> findByFriendUserIdAndRelationStatus(Integer userId, Integer relationStatus);
}