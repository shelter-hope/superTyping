package com.typinggame.chat.application;

import com.typinggame.chat.friends.CanMessageGuard;
import com.typinggame.chat.im.ImEventBus;
import com.typinggame.chat.im.MessagePolicy;
import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.chat.service.ChatAttachmentService;
import com.typinggame.dto.message.MessageDTO;
import com.typinggame.entity.message.ChatAttachment;
import com.typinggame.entity.message.FriendChatMessage;
import com.typinggame.entity.message.UserConversation;
import com.typinggame.enums.ReadStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.IntSupplier;

@Service
public class SendMessageService {
    private final FriendChatMessageRepository friendChatMessageRepository;
    private final CanMessageGuard canMessageGuard;
    private final ChatAttachmentService chatAttachmentService;
    private final ConversationSupport conversationSupport;
    private final MessageDtoMapper messageDtoMapper;
    private final ImEventBus imEventBus;

    public SendMessageService(FriendChatMessageRepository friendChatMessageRepository,
                              CanMessageGuard canMessageGuard,
                              ChatAttachmentService chatAttachmentService,
                              ConversationSupport conversationSupport,
                              MessageDtoMapper messageDtoMapper,
                              ImEventBus imEventBus) {
        this.friendChatMessageRepository = friendChatMessageRepository;
        this.canMessageGuard = canMessageGuard;
        this.chatAttachmentService = chatAttachmentService;
        this.conversationSupport = conversationSupport;
        this.messageDtoMapper = messageDtoMapper;
        this.imEventBus = imEventBus;
    }

    @Transactional
    public MessageDTO send(Integer senderId, Integer receiverId, String content, String type,
                           String clientMsgId, IntSupplier unreadCount) {
        int msgType = MessagePolicy.resolveType(type);
        String stored = MessageDtoMapper.normalizeContent(msgType, content);
        if (clientMsgId != null && !clientMsgId.isBlank()) {
            Optional<FriendChatMessage> existing =
                    friendChatMessageRepository.findFirstBySenderIdAndClientMsgId(senderId, clientMsgId);
            if (existing.isPresent()) {
                return messageDtoMapper.toDto(existing.get());
            }
        }
        canMessageGuard.assertCanMessage(senderId, receiverId);
        if (msgType == MessagePolicy.TYPE_IMAGE || msgType == MessagePolicy.TYPE_FILE) {
            ChatAttachment attachment = chatAttachmentService.requireForSend(senderId, stored, msgType);
            stored = String.valueOf(attachment.getId());
        }

        UserConversation senderConv = conversationSupport.getOrCreate(senderId, receiverId);
        FriendChatMessage message = newRow(senderId, receiverId, stored, msgType, senderConv.getId(), clientMsgId);
        friendChatMessageRepository.save(message);
        conversationSupport.updateLastMessage(senderConv, message);

        UserConversation receiverConv = conversationSupport.getOrCreate(receiverId, senderId);
        FriendChatMessage receiverMessage = newRow(senderId, receiverId, stored, msgType, receiverConv.getId(), clientMsgId);
        friendChatMessageRepository.save(receiverMessage);
        conversationSupport.updateLastMessage(receiverConv, message);

        MessageDTO senderView = messageDtoMapper.toDto(message);
        MessageDTO receiverView = messageDtoMapper.toDto(receiverMessage);
        imEventBus.pushMessage(receiverId, receiverView);
        imEventBus.pushMessage(senderId, senderView);
        imEventBus.pushUnread(receiverId, unreadCount.getAsInt());
        return senderView;
    }

    @Transactional
    public MessageDTO sendSystemNotice(Integer userId, String content, IntSupplier unreadCount) {
        MessagePolicy.requireContent(content);
        UserConversation conversation = conversationSupport.getOrCreate(userId, userId);
        FriendChatMessage message = newRow(userId, userId, content, MessagePolicy.TYPE_SYSTEM, conversation.getId(), null);
        friendChatMessageRepository.save(message);
        conversationSupport.updateLastMessage(conversation, message);
        MessageDTO dto = messageDtoMapper.toDto(message);
        imEventBus.pushMessage(userId, dto);
        imEventBus.pushUnread(userId, unreadCount.getAsInt());
        return dto;
    }

    private FriendChatMessage newRow(Integer senderId, Integer receiverId, String content, int msgType,
                                     Integer conversationId, String clientMsgId) {
        FriendChatMessage message = new FriendChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setMessageContent(content);
        message.setMessageType(msgType);
        message.setReadStatus(ReadStatus.UNREAD.getCode());
        message.setSendTime(LocalDateTime.now());
        message.setConversationId(conversationId);
        message.setClientMsgId(clientMsgId);
        message.setServerSeq(friendChatMessageRepository.maxServerSeq(conversationId) + 1);
        return message;
    }
}
