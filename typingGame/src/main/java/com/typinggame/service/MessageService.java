package com.typinggame.service;

import com.typinggame.dto.message.MessageDTO;

import java.util.List;

/**
 * 处理消息相关业务逻辑
 */
public interface MessageService {
    /**
     * 获取用户会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    List<MessageDTO> getConversations(Integer userId);

    /**
     * 获取会话消息
     * @param conversationId 会话ID
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    List<MessageDTO> getConversationMessages(Integer conversationId, Integer page, Integer size);

    /**
     * 按好友获取消息历史（基于当前用户与好友之间的会话）
     * @param userId 当前用户ID
     * @param friendId 好友用户ID
     * @param offset 页码（从0开始）
     * @return 消息列表
     */
    List<MessageDTO> getMessages(Integer userId, Integer friendId, Integer offset);

    /**
     * 发送消息
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param content 内容
     * @param type 类型
     * @return 消息DTO
     */
    MessageDTO sendMessage(Integer senderId, Integer receiverId, String content, String type);

    /**
     * 标记消息已读
     * @param messageId 消息ID
     * @return 是否成功
     */
    boolean markMessageAsRead(Integer messageId);

    /**
     * 将当前用户收到的所有未读消息批量标记为已读
     * @param userId 用户ID
     */
    void markAllAsRead(Integer userId);

    /**
     * 将当前用户与指定好友的会话中、自己收到的未读消息全部标记为已读（进入聊天时调用）
     * @param userId 当前用户ID（接收方）
     * @param friendId 好友用户ID（发送方）
     */
    void markAsReadByFriend(Integer userId, Integer friendId);

    /**
     * 删除消息
     * @param messageId 消息ID
     * @return 是否成功
     */
    boolean deleteMessage(Integer messageId);

    /**
     * 获取未读消息数
     * @param userId 用户ID
     * @return 未读数
     */
    Integer getUnreadCount(Integer userId);

    /**
     * 获取系统消息
     * @param userId 用户ID
     * @return 系统消息列表
     */
    List<MessageDTO> getSystemMessages(Integer userId);
}
