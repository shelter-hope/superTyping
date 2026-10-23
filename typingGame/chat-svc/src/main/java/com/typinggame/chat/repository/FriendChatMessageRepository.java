package com.typinggame.chat.repository;

import com.typinggame.entity.message.FriendChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendChatMessageRepository extends JpaRepository<FriendChatMessage, Integer>, JpaSpecificationExecutor<FriendChatMessage> {
    List<FriendChatMessage> findByConversationIdOrderBySendTimeDesc(Integer conversationId, Pageable pageable);
    List<FriendChatMessage> findByReceiverIdAndReadStatus(Integer receiverId, Integer readStatus);
    List<FriendChatMessage> findByReceiverIdAndMessageTypeOrderBySendTimeDesc(Integer receiverId, Integer messageType);
    int countByConversationIdAndReceiverIdAndReadStatus(Integer conversationId, Integer receiverId, Integer readStatus);
    int countByReceiverIdAndReadStatus(Integer receiverId, Integer readStatus);
    Optional<FriendChatMessage> findByIdAndReceiverId(Integer id, Integer receiverId);
    Optional<FriendChatMessage> findFirstBySenderIdAndClientMsgId(Integer senderId, String clientMsgId);
    List<FriendChatMessage> findByConversationIdAndIdLessThanOrderByIdDesc(Integer conversationId, Integer beforeId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("select coalesce(max(m.serverSeq), 0) from FriendChatMessage m where m.conversationId = :conversationId")
    int maxServerSeq(@org.springframework.data.repository.query.Param("conversationId") Integer conversationId);
}
