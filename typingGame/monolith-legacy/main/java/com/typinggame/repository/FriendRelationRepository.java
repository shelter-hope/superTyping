package com.typinggame.repository;

import com.typinggame.entity.message.FriendRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 好友关系表数据访问层
 */
@Repository
public interface FriendRelationRepository extends JpaRepository<FriendRelation, Integer> {

    /**
     * 查询当前用户作为 userId 或 friendUserId 且关系为好友的记录
     */
    @Query("SELECT f FROM FriendRelation f WHERE (f.userId = :userId OR f.friendUserId = :userId) AND f.relationStatus = :status")
    List<FriendRelation> findByUserIdOrFriendUserIdAndRelationStatus(@Param("userId") Integer userId, @Param("status") Integer status);

    /**
     *
     * @param senderId
     * @param receiverId
     * @param receiverId1
     * @param senderId1
     * @return
     */
    Optional<FriendRelation> findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(Integer senderId, Integer receiverId, Integer receiverId1, Integer senderId1);

    /**
     *
     * @param userId
     * @param code
     * @return
     */
    List<FriendRelation> findByFriendUserIdAndRelationStatus(Integer userId, Integer code);
}
