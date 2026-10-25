package com.typinggame.chat.application;

import com.typinggame.chat.im.MessagePolicy;
import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.chat.repository.SystemNotificationRepository;
import com.typinggame.chat.repository.UserConversationRepository;
import com.typinggame.dto.message.MessageDTO;
import com.typinggame.entity.message.UserConversation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class NotificationQueryService {
    private final FriendChatMessageRepository friendChatMessageRepository;
    private final SystemNotificationRepository systemNotificationRepository;
    private final UserConversationRepository userConversationRepository;
    private final MessageDtoMapper messageDtoMapper;

    public NotificationQueryService(FriendChatMessageRepository friendChatMessageRepository,
                                    SystemNotificationRepository systemNotificationRepository,
                                    UserConversationRepository userConversationRepository,
                                    MessageDtoMapper messageDtoMapper) {
        this.friendChatMessageRepository = friendChatMessageRepository;
        this.systemNotificationRepository = systemNotificationRepository;
        this.userConversationRepository = userConversationRepository;
        this.messageDtoMapper = messageDtoMapper;
    }

    public Integer getUnreadCount(Integer userId) {
        int conversationUnread = userConversationRepository.findByUserId(userId).stream()
                .map(UserConversation::getUnreadCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        long notificationUnread = systemNotificationRepository.countByUserIdAndIsRead(userId, false);
        return conversationUnread + (int) notificationUnread;
    }

    public List<MessageDTO> getSystemMessages(Integer userId) {
        List<MessageDTO> result = new ArrayList<>();
        friendChatMessageRepository.findByReceiverIdAndMessageTypeOrderBySendTimeDesc(userId, MessagePolicy.TYPE_SYSTEM)
                .forEach(m -> result.add(messageDtoMapper.toDto(m)));
        friendChatMessageRepository.findByReceiverIdAndMessageTypeOrderBySendTimeDesc(userId, MessagePolicy.TYPE_EMOJI)
                .stream()
                .filter(m -> m.getSenderId().equals(m.getReceiverId()))
                .forEach(m -> result.add(messageDtoMapper.toDto(m)));
        LocalDateTime now = LocalDateTime.now();
        systemNotificationRepository.findByUserIdOrderBySendTimeDesc(userId).stream()
                .filter(n -> n.getExpireTime() == null || n.getExpireTime().isAfter(now))
                .forEach(n -> result.add(messageDtoMapper.notificationToDto(n)));
        result.sort(Comparator.comparing(MessageDTO::getSendTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @Transactional
    public boolean markNotificationAsRead(Integer userId, Integer notificationId) {
        return systemNotificationRepository.findByIdAndUserId(notificationId, userId).map(n -> {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
                n.setReadTime(LocalDateTime.now());
                systemNotificationRepository.save(n);
            }
            return true;
        }).orElse(false);
    }
}
