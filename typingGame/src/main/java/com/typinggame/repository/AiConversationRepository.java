package com.typinggame.repository;

import com.typinggame.entity.message.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI对话会话表数据访问层
 */
@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Integer> {

    /** 按用户与机器人取最近更新的会话（用于默认会话） */
    Optional<AiConversation> findFirstByUser_UserIdAndAiRobotIdAndConversationTitleOrderByUpdateTimeDesc(
            Integer userId, Integer aiRobotId, String conversationTitle);
}
