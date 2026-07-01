package com.typinggame.chat.repository;

import com.typinggame.entity.message.FriendChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendChatMessageRepository extends JpaRepository<FriendChatMessage, Integer> {
    List<FriendChatMessage> findByConversationIdOrderBySendTimeDesc(Integer conversationId, Pageable pageable);
    List<FriendChatMessage> findByReceiverIdAndReadStatus(Integer receiverId, Integer readStatus);
    List<FriendChatMessage> findByReceiverIdAndMessageTypeOrderBySendTimeDesc(Integer receiverId, Integer messageType);
    int countByConversationIdAndReceiverIdAndReadStatus(Integer conversationId, Integer receiverId, Integer readStatus);
    int countByReceiverIdAndReadStatus(Integer receiverId, Integer readStatus);
}