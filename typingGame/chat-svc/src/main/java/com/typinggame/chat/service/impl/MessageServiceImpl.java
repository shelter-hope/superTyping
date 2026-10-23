package com.typinggame.chat.service.impl;

import com.typinggame.chat.im.ImEventPublisher;
import com.typinggame.chat.im.ImNotifier;
import com.typinggame.chat.im.MessagePolicy;
import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.chat.repository.FriendRelationRepository;
import com.typinggame.chat.repository.SystemNotificationRepository;
import com.typinggame.chat.repository.UserConversationRepository;
import com.typinggame.chat.repository.UserInfoRepository;
import com.typinggame.chat.service.MessageService;
import com.typinggame.dto.message.MessageDTO;
import com.typinggame.entity.message.FriendChatMessage;
import com.typinggame.entity.message.SystemNotification;
import com.typinggame.entity.message.UserConversation;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.enums.ReadStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private FriendChatMessageRepository friendChatMessageRepository;

    @Autowired
    private UserConversationRepository userConversationRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private FriendRelationRepository friendRelationRepository;

    @Autowired
    private ImEventPublisher imEventPublisher;

    @Autowired
    private ImNotifier imNotifier;

    @Autowired
    private SystemNotificationRepository systemNotificationRepository;

    private static final int TYPE_FRIEND = 1;
    private static final int TYPE_SYSTEM = 2;
    /** 前端据此区分 system_notification 记录，标记已读走单独接口 */
    static final String NOTIFICATION_TYPE = "notification";

    @Override
    public List<MessageDTO> getConversations(Integer userId) {
        List<UserConversation> conversations = userConversationRepository.findByUserId(userId);
        return conversations.stream().map(conversation -> {
            Optional<UserInfo> otherUserOptional = userInfoRepository.findById(conversation.getTargetUserId());
            if (otherUserOptional.isPresent()) {
                UserInfo otherUser = otherUserOptional.get();
                MessageDTO messageDTO = new MessageDTO();
                messageDTO.setId(conversation.getLastMessageId());
                messageDTO.setConversationId(conversation.getId());
                messageDTO.setSenderId(otherUser.getUserId());
                messageDTO.setSenderName(otherUser.getUsername());
                messageDTO.setSenderAvatar(otherUser.getAvatar());
                messageDTO.setReceiverId(userId);
                messageDTO.setReceiverName("current user");
                messageDTO.setContent(conversation.getLastMessageContent());
                messageDTO.setSendTime(conversation.getLastMessageTime());
                messageDTO.setUnreadCount(conversation.getUnreadCount());
                messageDTO.setIsRead(conversation.getUnreadCount() == 0);
                messageDTO.setType("friend");
                return messageDTO;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getConversationMessages(Integer userId, Integer conversationId, Integer page, Integer size) {
        if (!userConversationRepository.findByIdAndUserId(conversationId, userId).isPresent()) {
            return new java.util.ArrayList<>();
        }
        Pageable pageable = PageRequest.of(page, size);
        List<FriendChatMessage> messages = friendChatMessageRepository.findByConversationIdOrderBySendTimeDesc(conversationId, pageable);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getMessages(Integer userId, Integer friendId, Integer offset) {
        Optional<UserConversation> conversationOpt = userConversationRepository.findByUserIdAndTargetUserId(userId, friendId);
        if (!conversationOpt.isPresent()) {
            return new java.util.ArrayList<>();
        }
        Integer conversationId = conversationOpt.get().getId();
        int page = offset != null && offset >= 0 ? offset : 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);
        List<FriendChatMessage> messages = friendChatMessageRepository.findByConversationIdOrderBySendTimeDesc(conversationId, pageable);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MessageDTO sendMessage(Integer senderId, Integer receiverId, String content, String type) {
        return sendMessage(senderId, receiverId, content, type, null);
    }

    @Override
    @Transactional
    public MessageDTO sendMessage(Integer senderId, Integer receiverId, String content, String type, String clientMsgId) {
        MessagePolicy.requireChatType(type);
        MessagePolicy.requireContent(content);
        if (clientMsgId != null && !clientMsgId.isBlank()) {
            Optional<FriendChatMessage> existing = friendChatMessageRepository.findFirstBySenderIdAndClientMsgId(senderId, clientMsgId);
            if (existing.isPresent()) return convertToDTO(existing.get());
        }
        if (friendRelationRepository.findAccepted(senderId, receiverId).isEmpty()) {
            throw new IllegalArgumentException("只能给好友发消息");
        }
        int msgType = TYPE_FRIEND;

        FriendChatMessage message = new FriendChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setMessageContent(content);
        message.setMessageType(msgType);
        message.setReadStatus(ReadStatus.UNREAD.getCode());
        message.setSendTime(LocalDateTime.now());

        UserConversation senderConv = getOrCreateConversation(senderId, receiverId);
        message.setConversationId(senderConv.getId());
        message.setClientMsgId(clientMsgId);
        message.setServerSeq(friendChatMessageRepository.maxServerSeq(senderConv.getId()) + 1);
        friendChatMessageRepository.save(message);

        updateConversation(senderConv, message);

        UserConversation receiverConv = getOrCreateConversation(receiverId, senderId);
        updateConversation(receiverConv, message);

        FriendChatMessage receiverMessage = new FriendChatMessage();
        receiverMessage.setSenderId(senderId);
        receiverMessage.setReceiverId(receiverId);
        receiverMessage.setMessageContent(content);
        receiverMessage.setMessageType(msgType);
        receiverMessage.setReadStatus(ReadStatus.UNREAD.getCode());
        receiverMessage.setSendTime(LocalDateTime.now());
        receiverMessage.setConversationId(receiverConv.getId());
        receiverMessage.setClientMsgId(clientMsgId);
        receiverMessage.setServerSeq(friendChatMessageRepository.maxServerSeq(receiverConv.getId()) + 1);
        friendChatMessageRepository.save(receiverMessage);

        MessageDTO senderView = convertToDTO(message);
        MessageDTO receiverView = convertToDTO(receiverMessage);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", "message");
        event.put("userId", senderId);
        event.put("receiverId", receiverId);
        event.put("message", senderView);
        imEventPublisher.publish(event);
        imEventPublisher.publish(Map.of("event", "unread", "userId", receiverId, "count", getUnreadCount(receiverId)));

        imNotifier.pushMessage(receiverId, receiverView);
        imNotifier.pushMessage(senderId, senderView);

        return senderView;
    }

    @Override
    public List<MessageDTO> getMessagesBefore(Integer userId, Integer friendId, Integer beforeId, int limit) {
        Optional<UserConversation> conversationOpt = userConversationRepository.findByUserIdAndTargetUserId(userId, friendId);
        if (conversationOpt.isEmpty()) return List.of();
        int size = Math.max(1, Math.min(50, limit));
        List<FriendChatMessage> messages = beforeId == null
                ? friendChatMessageRepository.findByConversationIdOrderBySendTimeDesc(conversationOpt.get().getId(), PageRequest.of(0, size))
                : friendChatMessageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(conversationOpt.get().getId(), beforeId, PageRequest.of(0, size));
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MessageDTO sendSystemNotice(Integer userId, String content) {
        MessagePolicy.requireContent(content);
        FriendChatMessage message = new FriendChatMessage();
        message.setSenderId(userId);
        message.setReceiverId(userId);
        message.setMessageContent(content);
        message.setMessageType(TYPE_SYSTEM);
        message.setReadStatus(ReadStatus.UNREAD.getCode());
        message.setSendTime(LocalDateTime.now());
        UserConversation conversation = getOrCreateConversation(userId, userId);
        message.setConversationId(conversation.getId());
        message.setServerSeq(friendChatMessageRepository.maxServerSeq(conversation.getId()) + 1);
        friendChatMessageRepository.save(message);
        updateConversation(conversation, message);
        MessageDTO dto = convertToDTO(message);
        imEventPublisher.publish(Map.of("event", "notification", "userId", userId, "message", dto));
        imEventPublisher.publish(Map.of("event", "unread", "userId", userId, "count", getUnreadCount(userId)));
        imNotifier.pushMessage(userId, dto);
        return dto;
    }

    private UserConversation getOrCreateConversation(Integer userId, Integer targetUserId) {
        return userConversationRepository.findByUserIdAndTargetUserId(userId, targetUserId)
                .orElseGet(() -> {
                    UserConversation c = new UserConversation();
                    c.setUserId(userId);
                    c.setTargetUserId(targetUserId);
                    c.setUnreadCount(0);
                    userConversationRepository.save(c);
                    return c;
                });
    }

    private void updateConversation(UserConversation conversation, FriendChatMessage message) {
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageContent(message.getMessageContent());
        conversation.setLastMessageTime(message.getSendTime());
        if (conversation.getUserId().equals(message.getReceiverId())) {
            conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        }
        userConversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public boolean markMessageAsRead(Integer userId, Integer messageId) {
        Optional<FriendChatMessage> messageOptional = friendChatMessageRepository.findByIdAndReceiverId(messageId, userId);
        if (messageOptional.isPresent()) {
            FriendChatMessage message = messageOptional.get();
            if (!Objects.equals(message.getReadStatus(), ReadStatus.READ.getCode())) {
                message.setReadStatus(ReadStatus.READ.getCode());
                friendChatMessageRepository.save(message);

                Optional<UserConversation> convOpt = userConversationRepository.findByIdAndUserId(message.getConversationId(), userId);
                convOpt.ifPresent(conv -> {
                    if (conv.getUnreadCount() > 0) {
                        conv.setUnreadCount(conv.getUnreadCount() - 1);
                        userConversationRepository.save(conv);
                    }
                });
                return true;
            }
        }
        return false;
    }

    @Override
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
        for (FriendChatMessage m : unreadMessages) {
            m.setReadStatus(ReadStatus.READ.getCode());
            m.setReadTime(now);
        }
        friendChatMessageRepository.saveAll(unreadMessages);

        for (Integer conversationId : byConversation.keySet()) {
            Optional<UserConversation> convOpt = userConversationRepository.findById(conversationId);
            if (convOpt.isPresent()) {
                UserConversation conv = convOpt.get();
                if (!conv.getUserId().equals(userId)) {
                    continue;
                }
                int unreadCount = friendChatMessageRepository
                        .countByConversationIdAndReceiverIdAndReadStatus(conversationId, userId, ReadStatus.UNREAD.getCode());
                conv.setUnreadCount(unreadCount);
                userConversationRepository.save(conv);
            }
        }
    }

    @Override
    @Transactional
    public void markAsReadByFriend(Integer userId, Integer friendId) {
        Optional<UserConversation> convOpt = userConversationRepository.findByUserIdAndTargetUserId(userId, friendId);
        if (!convOpt.isPresent()) {
            return;
        }
        Integer conversationId = convOpt.get().getId();
        Integer friendConversationId = userConversationRepository.findByUserIdAndTargetUserId(friendId, userId)
                .map(UserConversation::getId)
                .orElse(null);
        List<FriendChatMessage> unread = friendChatMessageRepository.findByReceiverIdAndReadStatus(userId, ReadStatus.UNREAD.getCode()).stream()
                .filter(m -> conversationId.equals(m.getConversationId())
                        || (friendConversationId != null && friendConversationId.equals(m.getConversationId())))
                .collect(Collectors.toList());

        if (!unread.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (FriendChatMessage m : unread) {
                m.setReadStatus(ReadStatus.READ.getCode());
                m.setReadTime(now);
            }
            friendChatMessageRepository.saveAll(unread);
        }

        int unreadCount = friendChatMessageRepository
                .countByConversationIdAndReceiverIdAndReadStatus(conversationId, userId, ReadStatus.UNREAD.getCode());
        UserConversation conv = convOpt.get();
        conv.setUnreadCount(unreadCount);
        userConversationRepository.save(conv);

        if (!unread.isEmpty()) {
            imNotifier.pushRead(friendId, userId);
        }
    }

    @Override
    public boolean deleteMessage(Integer userId, Integer messageId) {
        Optional<FriendChatMessage> messageOptional = friendChatMessageRepository.findById(messageId);
        if (messageOptional.isPresent()) {
            FriendChatMessage message = messageOptional.get();
            if (!Objects.equals(userId, message.getSenderId()) && !Objects.equals(userId, message.getReceiverId())) {
                return false;
            }
            friendChatMessageRepository.delete(message);
            return true;
        }
        return false;
    }

    @Override
    public Integer getUnreadCount(Integer userId) {
        int conversationUnread = userConversationRepository.findByUserId(userId).stream()
                .map(UserConversation::getUnreadCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        long notificationUnread = systemNotificationRepository.countByUserIdAndIsRead(userId, false);
        return conversationUnread + (int) notificationUnread;
    }

    @Override
    public List<MessageDTO> getSystemMessages(Integer userId) {
        List<MessageDTO> result = new ArrayList<>();
        friendChatMessageRepository.findByReceiverIdAndMessageTypeOrderBySendTimeDesc(userId, TYPE_SYSTEM)
                .forEach(m -> result.add(convertToDTO(m)));
        LocalDateTime now = LocalDateTime.now();
        systemNotificationRepository.findByUserIdOrderBySendTimeDesc(userId).stream()
                .filter(n -> n.getExpireTime() == null || n.getExpireTime().isAfter(now))
                .forEach(n -> result.add(notificationToDTO(n)));
        result.sort(Comparator.comparing(MessageDTO::getSendTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @Override
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

    private MessageDTO notificationToDTO(SystemNotification n) {
        MessageDTO dto = new MessageDTO();
        dto.setId(n.getId());
        dto.setSenderId(0);
        dto.setSenderName("系统");
        dto.setReceiverId(n.getUserId());
        dto.setContent(n.getNotificationTitle() + "：" + n.getNotificationContent());
        dto.setSendTime(n.getSendTime());
        dto.setIsRead(Boolean.TRUE.equals(n.getIsRead()));
        dto.setType(NOTIFICATION_TYPE);
        return dto;
    }

    private MessageDTO convertToDTO(FriendChatMessage message) {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(message.getId());
        messageDTO.setConversationId(message.getConversationId());
        messageDTO.setSenderId(message.getSenderId());

        userInfoRepository.findById(message.getSenderId()).ifPresent(u -> {
            messageDTO.setSenderName(u.getUsername());
            messageDTO.setSenderAvatar(u.getAvatar());
        });

        messageDTO.setReceiverId(message.getReceiverId());
        userInfoRepository.findById(message.getReceiverId()).ifPresent(u ->
                messageDTO.setReceiverName(u.getUsername())
        );

        messageDTO.setContent(message.getMessageContent());
        messageDTO.setSendTime(message.getSendTime());
        messageDTO.setIsRead(Objects.equals(message.getReadStatus(), ReadStatus.READ.getCode()));
        messageDTO.setType(message.getMessageType() == TYPE_SYSTEM ? "system" : "friend");
        messageDTO.setClientMsgId(message.getClientMsgId());
        messageDTO.setServerSeq(message.getServerSeq());
        return messageDTO;
    }
}
