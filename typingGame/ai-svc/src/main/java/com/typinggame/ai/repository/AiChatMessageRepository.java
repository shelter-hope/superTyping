package com.typinggame.ai.repository;

import com.typinggame.entity.message.AiChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Integer>, JpaSpecificationExecutor<AiChatMessage> {
    List<AiChatMessage> findByConversation_IdOrderBySendTimeDesc(Integer conversationId, Pageable pageable);
}