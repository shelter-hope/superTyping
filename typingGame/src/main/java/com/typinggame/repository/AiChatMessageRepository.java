package com.typinggame.repository;

import com.typinggame.entity.message.AiChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI聊天消息表数据访问层
 */
@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Integer> {

    /** 按会话取最近消息（用于上下文或历史） */
    List<AiChatMessage> findByConversation_IdOrderBySendTimeDesc(Integer conversationId, Pageable pageable);
}
