package com.typinggame.ai.repository;

import com.typinggame.entity.message.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Integer> {
    Optional<AiConversation> findFirstByUser_UserIdAndAiRobotIdAndConversationTitleOrderByUpdateTimeDesc(
            Integer userId, Integer aiRobotId, String conversationTitle);
}