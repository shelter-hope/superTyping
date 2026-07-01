package com.typinggame.repository;

import com.typinggame.entity.message.FriendChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 好友聊天消息表数据访问层
 */
@Repository
public interface FriendChatMessageRepository extends JpaRepository<FriendChatMessage, Integer> {
    /**
     * 分页获取会话中的消息列表
     * @param conversationId 会话ID
     * @param pageable 分页数据
     * @return 消息列表
     */
    List<FriendChatMessage> findByConversationIdOrderBySendTimeDesc(Integer conversationId, Pageable pageable);

    /**
     * 获取用户已读/未读消息的详细数量
     * @param userId 用户
     * @param code 阅读状态码
     * @return 数量
     */
    Integer countByReceiverIdAndReadStatus(Integer userId, Integer code);

    /**
     * 获取系统消息
     * @param receiverId 接收者ID
     * @param messageType 消息类型
     * @return 消息列表
     */
    List<FriendChatMessage> findByReceiverIdAndMessageTypeOrderBySendTimeDesc(Integer receiverId, Integer messageType);

    /**
     * 查询某用户收到的所有未读消息
     * @param receiverId 接收者ID
     * @param readStatus 阅读状态
     * @return 未读消息列表
     */
    List<FriendChatMessage> findByReceiverIdAndReadStatus(Integer receiverId, Integer readStatus);

    /**
     * 统计某会话下某用户的未读消息数
     * @param conversationId 会话ID
     * @param receiverId 接收者ID
     * @param readStatus 阅读状态
     * @return 未读消息数量
     */
    Integer countByConversationIdAndReceiverIdAndReadStatus(Integer conversationId, Integer receiverId, Integer readStatus);
}
