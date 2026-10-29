package com.typinggame.chat.service.impl;

import com.typinggame.chat.service.application.ConversationQueryService;
import com.typinggame.chat.service.application.MessageReadService;
import com.typinggame.chat.service.application.NotificationQueryService;
import com.typinggame.chat.service.application.RecallMessageService;
import com.typinggame.chat.service.application.SendMessageService;
import com.typinggame.chat.service.MessageService;
import com.typinggame.dto.message.MessageDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/** 消息门面：对外保持 MessageService 契约，内部委托 application 服务。 */
@Service
public class MessageServiceImpl implements MessageService {
    private final SendMessageService sendMessageService;
    private final RecallMessageService recallMessageService;
    private final ConversationQueryService conversationQueryService;
    private final MessageReadService messageReadService;
    private final NotificationQueryService notificationQueryService;

    public MessageServiceImpl(SendMessageService sendMessageService,
                              RecallMessageService recallMessageService,
                              ConversationQueryService conversationQueryService,
                              MessageReadService messageReadService,
                              NotificationQueryService notificationQueryService) {
        this.sendMessageService = sendMessageService;
        this.recallMessageService = recallMessageService;
        this.conversationQueryService = conversationQueryService;
        this.messageReadService = messageReadService;
        this.notificationQueryService = notificationQueryService;
    }

    @Override
    public List<MessageDTO> getConversations(Integer userId) {
        return conversationQueryService.getConversations(userId);
    }

    @Override
    public List<MessageDTO> getConversationMessages(Integer userId, Integer conversationId, Integer page, Integer size) {
        return conversationQueryService.getConversationMessages(userId, conversationId, page, size);
    }

    @Override
    public List<MessageDTO> getMessages(Integer userId, Integer friendId, Integer offset) {
        return conversationQueryService.getMessages(userId, friendId, offset);
    }

    @Override
    public MessageDTO sendMessage(Integer senderId, Integer receiverId, String content, String type) {
        return sendMessage(senderId, receiverId, content, type, null);
    }

    @Override
    public MessageDTO sendMessage(Integer senderId, Integer receiverId, String content, String type, String clientMsgId) {
        return sendMessageService.send(senderId, receiverId, content, type, clientMsgId,
                () -> notificationQueryService.getUnreadCount(receiverId));
    }

    @Override
    public List<MessageDTO> getMessagesBefore(Integer userId, Integer friendId, Integer beforeId, int limit) {
        return conversationQueryService.getMessagesBefore(userId, friendId, beforeId, limit);
    }

    @Override
    public MessageDTO sendSystemNotice(Integer userId, String content) {
        return sendMessageService.sendSystemNotice(userId, content,
                () -> notificationQueryService.getUnreadCount(userId));
    }

    @Override
    public boolean markMessageAsRead(Integer userId, Integer messageId) {
        return messageReadService.markMessageAsRead(userId, messageId);
    }

    @Override
    public void markAllAsRead(Integer userId) {
        messageReadService.markAllAsRead(userId);
    }

    @Override
    public void markAsReadByFriend(Integer userId, Integer friendId) {
        messageReadService.markAsReadByFriend(userId, friendId,
                () -> notificationQueryService.getUnreadCount(userId));
    }

    @Override
    public boolean deleteMessage(Integer userId, Integer messageId) {
        return messageReadService.deleteMessage(userId, messageId);
    }

    @Override
    public MessageDTO recall(Integer userId, Integer messageId) {
        return recallMessageService.recall(userId, messageId);
    }

    @Override
    public Integer getUnreadCount(Integer userId) {
        return notificationQueryService.getUnreadCount(userId);
    }

    @Override
    public List<MessageDTO> getSystemMessages(Integer userId) {
        return notificationQueryService.getSystemMessages(userId);
    }

    @Override
    public boolean markNotificationAsRead(Integer userId, Integer notificationId) {
        return notificationQueryService.markNotificationAsRead(userId, notificationId);
    }
}
