package com.typinggame.chat.repository;

import com.typinggame.entity.message.FriendRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRelationRepository extends JpaRepository<FriendRelation, Integer> {
    @Query("""
            select f from FriendRelation f
            where f.relationStatus = 1
              and ((f.userId = :left and f.friendUserId = :right)
                or (f.userId = :right and f.friendUserId = :left))
            """)
    Optional<FriendRelation> findAccepted(@Param("left") Integer left, @Param("right") Integer right);

    @Query("""
            select case when f.userId = :userId then f.friendUserId else f.userId end
            from FriendRelation f
            where f.relationStatus = 1
              and (f.userId = :userId or f.friendUserId = :userId)
            """)
    List<Integer> findFriendIds(@Param("userId") Integer userId);
}