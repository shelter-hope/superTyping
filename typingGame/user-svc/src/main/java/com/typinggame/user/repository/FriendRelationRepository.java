package com.typinggame.user.repository;

import com.typinggame.entity.message.FriendRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRelationRepository extends JpaRepository<FriendRelation, Integer>, JpaSpecificationExecutor<FriendRelation> {

    @Query("""
            select f from FriendRelation f
            where f.relationStatus = :status
              and (f.userId = :userId or f.friendUserId = :userId)
            """)
    List<FriendRelation> findInvolving(@Param("userId") Integer userId, @Param("status") Integer status);

    Optional<FriendRelation> findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(Integer userId1, Integer userId2, Integer userId3, Integer userId4);

    List<FriendRelation> findByFriendUserIdAndRelationStatus(Integer userId, Integer relationStatus);
}