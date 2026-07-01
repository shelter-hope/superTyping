package com.typinggame.chat.service;

import com.typinggame.dto.message.MessageDTO;

import java.util.List;

public interface MessageService {
    List<MessageDTO> getConversations(Integer userId);
    List<MessageDTO> getConversationMessages(Integer conversationId, Integer page, Integer size);
    List<MessageDTO> getMessages(Integer userId, Integer friendId, Integer offset);
    MessageDTO sendMessage(Integer senderId, Integer receiverId, String content, String type);
    boolean markMessageAsRead(Integer messageId);
    void markAllAsRead(Integer userId);
    void markAsReadByFriend(Integer userId, Integer friendId);
    boolean deleteMessage(Integer messageId);
    Integer getUnreadCount(Integer userId);
    List<MessageDTO> getSystemMessages(Integer userId);
}