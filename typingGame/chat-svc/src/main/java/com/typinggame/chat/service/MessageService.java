package com.typinggame.chat.service;

import com.typinggame.dto.message.MessageDTO;

import java.util.List;

public interface MessageService {
    List<MessageDTO> getConversations(Integer userId);
    List<MessageDTO> getConversationMessages(Integer userId, Integer conversationId, Integer page, Integer size);
    List<MessageDTO> getMessages(Integer userId, Integer friendId, Integer offset);
    MessageDTO sendMessage(Integer senderId, Integer receiverId, String content, String type);
    MessageDTO sendMessage(Integer senderId, Integer receiverId, String content, String type, String clientMsgId);
    List<MessageDTO> getMessagesBefore(Integer userId, Integer friendId, Integer beforeId, int limit);
    MessageDTO sendSystemNotice(Integer userId, String content);
    boolean markMessageAsRead(Integer userId, Integer messageId);
    void markAllAsRead(Integer userId);
    void markAsReadByFriend(Integer userId, Integer friendId);
    boolean deleteMessage(Integer userId, Integer messageId);
    MessageDTO recall(Integer userId, Integer messageId);
    Integer getUnreadCount(Integer userId);
    List<MessageDTO> getSystemMessages(Integer userId);
    boolean markNotificationAsRead(Integer userId, Integer notificationId);
}
