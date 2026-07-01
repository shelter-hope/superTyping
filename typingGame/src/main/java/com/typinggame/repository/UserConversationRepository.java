package com.typinggame.repository;

import com.typinggame.entity.message.UserConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户好友对话会话表数据访问层
 */
@Repository
public interface UserConversationRepository extends JpaRepository<UserConversation, Integer> {
    /**
     *
     * @param userId
     * @return
     */
    List<UserConversation> findByUserId(Integer userId);

    /**
     *
     * @param senderId
     * @param receiverId
     * @return
     */
    Optional<UserConversation> findByUserIdAndTargetUserId(Integer senderId, Integer receiverId);
}
