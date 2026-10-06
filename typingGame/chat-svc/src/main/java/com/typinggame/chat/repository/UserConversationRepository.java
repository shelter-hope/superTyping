package com.typinggame.chat.repository;

import com.typinggame.entity.message.UserConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConversationRepository extends JpaRepository<UserConversation, Integer> {
    List<UserConversation> findByUserId(Integer userId);
    Optional<UserConversation> findByUserIdAndTargetUserId(Integer userId, Integer targetUserId);
    Optional<UserConversation> findByIdAndUserId(Integer id, Integer userId);
}
