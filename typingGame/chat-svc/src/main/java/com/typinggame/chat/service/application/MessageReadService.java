package com.typinggame.chat.service.application;

import com.typinggame.chat.support.im.ImEventBus;
import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.chat.repository.UserConversationRepository;
import com.typinggame.chat.entity.FriendChatMessage;
import com.typinggame.chat.entity.UserConversation;
import com.typinggame.enums.ReadStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

@Service
public class MessageReadService {
    private final FriendChatMessageRepository friendChatMessageRepository;
    private final UserConversationRepository userConversationRepository;
    private final ImEventBus imEventBus;

    public MessageReadService(FriendChatMessageRepository friendChatMessageRepository,
                              UserConversationRepository userConversationRepository,
                              ImEventBus imEventBus) {
        this.friendChatMessageRepository = friendChatMessageRepository;
        this.userConversationRepository = userConversationRepository;
        this.imEventBus = imEventBus;
    }

    @Transactional
    public boolean markMessageAsRead(Integer userId, Integer messageId) {
        Optional<FriendChatMessage> messageOptional =
                friendChatMessageRepository.findByIdAndReceiverId(messageId, userId);
        if (messageOptional.isEmpty()) {
            return false;
        }
        FriendChatMessage message = messageOptional.get();
        if (Objects.equals(message.getReadStatus(), ReadStatus.READ.getCode())) {
            return false;
        }
        message.setReadStatus(ReadStatus.READ.getCode());
        friendChatMessageRepository.save(message);
        userConversationRepository.findByIdAndUserId(message.getConversationId(), userId).ifPresent(conv -> {
            if (conv.getUnreadCount() > 0) {
                conv.setUnreadCount(conv.getUnreadCount() - 1);
                userConversationRepository.save(conv);
            }
        });
        return true;
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        List<FriendChatMessage> unreadMessages =
                friendChatMessageRepository.findByReceiverIdAndReadStatus(userId, ReadStatus.UNREAD.getCode());
        if (unreadMessages.isEmpty()) {
            return;
        }
        var byConversation = unreadMessages.stream()
                .collect(Collectors.groupingBy(FriendChatMessage::getConversationId));
        LocalDateTime now = LocalDateTime.now();
        for (FriendChatMessage message : unreadMessages) {
            message.setReadStatus(ReadStatus.READ.getCode());
            message.setReadTime(now);
        }
        friendChatMessageRepository.saveAll(unreadMessages);
        for (Integer conversationId : byConversation.keySet()) {
            Optional<UserConversation> convOpt = userConversationRepository.findById(conversationId);
            if (convOpt.isEmpty()) {
                continue;
            }
            UserConversation conv = convOpt.get();
            if (!conv.getUserId().equals(userId)) {
                continue;
            }
            int unreadCount = friendChatMessageRepository
                    .countByConversationIdAndReceiverIdAndReadStatus(
                            conversationId, userId, ReadStatus.UNREAD.getCode());
            conv.setUnreadCount(unreadCount);
            userConversationRepository.save(conv);
        }
    }

    @Transactional
    public void markAsReadByFriend(Integer userId, Integer friendId, IntSupplier unreadCount) {
        Optional<UserConversation> convOpt =
                userConversationRepository.findByUserIdAndTargetUserId(userId, friendId);
        if (convOpt.isEmpty()) {
            return;
        }
        Integer conversationId = convOpt.get().getId();
        Integer friendConversationId = userConversationRepository.findByUserIdAndTargetUserId(friendId, userId)
                .map(UserConversation::getId)
                .orElse(null);
        List<FriendChatMessage> unread = friendChatMessageRepository
                .findByReceiverIdAndReadStatus(userId, ReadStatus.UNREAD.getCode()).stream()
                .filter(m -> conversationId.equals(m.getConversationId())
                        || (friendConversationId != null && friendConversationId.equals(m.getConversationId())))
                .collect(Collectors.toList());
        if (!unread.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (FriendChatMessage message : unread) {
                message.setReadStatus(ReadStatus.READ.getCode());
                message.setReadTime(now);
            }
            friendChatMessageRepository.saveAll(unread);
        }
        int count = friendChatMessageRepository
                .countByConversationIdAndReceiverIdAndReadStatus(
                        conversationId, userId, ReadStatus.UNREAD.getCode());
        UserConversation conv = convOpt.get();
        conv.setUnreadCount(count);
        userConversationRepository.save(conv);
        if (!unread.isEmpty()) {
            imEventBus.pushRead(friendId, userId);
        }
        imEventBus.pushUnread(userId, unreadCount.getAsInt());
    }

    public boolean deleteMessage(Integer userId, Integer messageId) {
        Optional<FriendChatMessage> messageOptional = friendChatMessageRepository.findById(messageId);
        if (messageOptional.isEmpty()) {
            return false;
        }
        FriendChatMessage message = messageOptional.get();
        if (!Objects.equals(userId, message.getSenderId()) && !Objects.equals(userId, message.getReceiverId())) {
            return false;
        }
        friendChatMessageRepository.delete(message);
        return true;
    }
}
