package com.typinggame.ai.repository;

import com.typinggame.ai.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Integer>, JpaSpecificationExecutor<AiConversation> {
    Optional<AiConversation> findFirstByUserIdAndAiRobotIdAndConversationTitleOrderByUpdateTimeDesc(
            Integer userId, Integer aiRobotId, String conversationTitle);
}