package com.typinggame.chat.service.impl;

import com.typinggame.chat.im.ImNotifier;
import com.typinggame.chat.im.MessagePolicy;
import com.typinggame.chat.repository.ChatAttachmentRepository;
import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.chat.repository.FriendRelationRepository;
import com.typinggame.chat.repository.SystemNotificationRepository;
import com.typinggame.chat.repository.UserConversationRepository;
import com.typinggame.chat.repository.UserInfoRepository;
import com.typinggame.chat.service.ChatAttachmentService;
import com.typinggame.chat.service.MessageService;
import com.typinggame.common.exception.BusinessException;
import com.typinggame.dto.message.MessageDTO;
import com.typinggame.entity.message.ChatAttachment;
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
import java.util.List;
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
    private ImNotifier imNotifier;

    @Autowired
    private SystemNotificationRepository systemNotificationRepository;

    @Autowired
    private ChatAttachmentService chatAttachmentService;

    @Autowired
    private ChatAttachmentRepository chatAttachmentRepository;

    /** 前端据此区分 system_notification 记录，标记已读走单独接口 */
    static final String NOTIFICATION_TYPE = "notification";
    private static final int RECALL_MINUTES = 2;

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
        int msgType = MessagePolicy.resolveType(type);
        String stored = normalizeContent(msgType, content);
        if (clientMsgId != null && !clientMsgId.isBlank()) {
            Optional<FriendChatMessage> existing = friendChatMessageRepository.findFirstBySenderIdAndClientMsgId(senderId, clientMsgId);
            if (existing.isPresent()) return convertToDTO(existing.get());
        }
        if (friendRelationRepository.findAccepted(senderId, receiverId).isEmpty()) {
            throw new IllegalArgumentException("只能给好友发消息");
        }
        if (msgType == MessagePolicy.TYPE_IMAGE || msgType == MessagePolicy.TYPE_FILE) {
            ChatAttachment attachment = chatAttachmentService.requireForSend(senderId, stored, msgType);
            stored = String.valueOf(attachment.getId());
        }

        FriendChatMessage message = new FriendChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setMessageContent(stored);
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
        receiverMessage.setMessageContent(stored);
        receiverMessage.setMessageType(msgType);
        receiverMessage.setReadStatus(ReadStatus.UNREAD.getCode());
        receiverMessage.setSendTime(LocalDateTime.now());
        receiverMessage.setConversationId(receiverConv.getId());
        receiverMessage.setClientMsgId(clientMsgId);
        receiverMessage.setServerSeq(friendChatMessageRepository.maxServerSeq(receiverConv.getId()) + 1);
        friendChatMessageRepository.save(receiverMessage);

        MessageDTO senderView = convertToDTO(message);
        MessageDTO receiverView = convertToDTO(receiverMessage);
        imNotifier.pushMessage(receiverId, receiverView);
        imNotifier.pushMessage(senderId, senderView);
        imNotifier.pushUnread(receiverId, getUnreadCount(receiverId));
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
        message.setMessageType(MessagePolicy.TYPE_SYSTEM);
        message.setReadStatus(ReadStatus.UNREAD.getCode());
        message.setSendTime(LocalDateTime.now());
        UserConversation conversation = getOrCreateConversation(userId, userId);
        message.setConversationId(conversation.getId());
        message.setServerSeq(friendChatMessageRepository.maxServerSeq(conversation.getId()) + 1);
        friendChatMessageRepository.save(message);
        updateConversation(conversation, message);
        MessageDTO dto = convertToDTO(message);
        imNotifier.pushMessage(userId, dto);
        imNotifier.pushUnread(userId, getUnreadCount(userId));
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
        conversation.setLastMessageContent(preview(message));
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
        imNotifier.pushUnread(userId, getUnreadCount(userId));
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
    @Transactional
    public MessageDTO recall(Integer userId, Integer messageId) {
        FriendChatMessage message = friendChatMessageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("消息不存在"));
        if (!userId.equals(message.getSenderId())) {
            throw BusinessException.forbidden("只能撤回自己的消息");
        }
        if (Boolean.TRUE.equals(message.getIsRecall())) {
            return convertToDTO(message);
        }
        LocalDateTime sentAt = message.getSendTime();
        if (sentAt == null || LocalDateTime.now().isAfter(sentAt.plusMinutes(RECALL_MINUTES))) {
            throw BusinessException.badRequest("已超过撤回时间");
        }
        List<FriendChatMessage> rows = peerRows(message);
        LocalDateTime now = LocalDateTime.now();
        for (FriendChatMessage row : rows) {
            row.setIsRecall(true);
            row.setRecallTime(now);
        }
        friendChatMessageRepository.saveAll(rows);
        java.util.Set<Integer> recalledIds = rows.stream().map(FriendChatMessage::getId).collect(Collectors.toSet());
        List<UserConversation> conversations = new ArrayList<>();
        userConversationRepository.findByUserIdAndTargetUserId(message.getSenderId(), message.getReceiverId())
                .ifPresent(conversations::add);
        userConversationRepository.findByUserIdAndTargetUserId(message.getReceiverId(), message.getSenderId())
                .ifPresent(conversations::add);
        for (UserConversation conversation : conversations) {
            if (recalledIds.contains(conversation.getLastMessageId())) {
                conversation.setLastMessageContent("已撤回");
                userConversationRepository.save(conversation);
            }
        }
        imNotifier.pushRecall(List.of(message.getSenderId(), message.getReceiverId()), message.getClientMsgId());
        return convertToDTO(message);
    }

    private List<FriendChatMessage> peerRows(FriendChatMessage message) {
        if (message.getClientMsgId() != null && !message.getClientMsgId().isBlank()) {
            List<FriendChatMessage> rows = friendChatMessageRepository.findBySenderIdAndClientMsgId(
                    message.getSenderId(), message.getClientMsgId());
            if (!rows.isEmpty()) {
                return rows;
            }
        }
        LocalDateTime from = message.getSendTime().minusSeconds(2);
        LocalDateTime to = message.getSendTime().plusSeconds(2);
        List<FriendChatMessage> rows = new ArrayList<>(friendChatMessageRepository
                .findBySenderIdAndReceiverIdAndMessageContent(
                        message.getSenderId(), message.getReceiverId(), message.getMessageContent())
                .stream()
                .filter(row -> row.getSendTime() != null
                        && !row.getSendTime().isBefore(from)
                        && !row.getSendTime().isAfter(to))
                .toList());
        if (rows.stream().noneMatch(row -> row.getId().equals(message.getId()))) {
            rows.add(message);
        }
        return rows;
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
        friendChatMessageRepository.findByReceiverIdAndMessageTypeOrderBySendTimeDesc(userId, MessagePolicy.TYPE_SYSTEM)
                .forEach(m -> result.add(convertToDTO(m)));
        friendChatMessageRepository.findByReceiverIdAndMessageTypeOrderBySendTimeDesc(userId, MessagePolicy.TYPE_EMOJI)
                .stream()
                .filter(m -> m.getSenderId().equals(m.getReceiverId()))
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

        boolean recalled = Boolean.TRUE.equals(message.getIsRecall());
        int messageType = message.getMessageType() == null ? MessagePolicy.TYPE_TEXT : message.getMessageType();
        boolean selfAddressed = message.getSenderId() != null && message.getSenderId().equals(message.getReceiverId());
        messageDTO.setRecalled(recalled);
        messageDTO.setContent(recalled ? "" : message.getMessageContent());
        messageDTO.setSendTime(message.getSendTime());
        messageDTO.setIsRead(Objects.equals(message.getReadStatus(), ReadStatus.READ.getCode()));
        messageDTO.setType(MessagePolicy.typeName(messageType, selfAddressed));
        messageDTO.setClientMsgId(message.getClientMsgId());
        messageDTO.setServerSeq(message.getServerSeq());
        if (!recalled && (messageType == MessagePolicy.TYPE_IMAGE || messageType == MessagePolicy.TYPE_FILE)) {
            attachmentOf(message.getMessageContent()).ifPresent(attachment -> {
                messageDTO.setAttachmentName(attachment.getOriginalName());
                messageDTO.setAttachmentMediaType(attachment.getMediaType());
                messageDTO.setAttachmentSize(attachment.getSizeBytes());
            });
        }
        return messageDTO;
    }

    private Optional<ChatAttachment> attachmentOf(String content) {
        if (content == null || chatAttachmentRepository == null) {
            return Optional.empty();
        }
        try {
            return chatAttachmentRepository.findById(Integer.parseInt(content.trim()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static String normalizeContent(int messageType, String content) {
        MessagePolicy.requireContent(content);
        String value = content.trim();
        if (messageType == MessagePolicy.TYPE_EMOJI) {
            MessagePolicy.requireEmoji(value);
            return value;
        }
        return content;
    }

    private static String preview(FriendChatMessage message) {
        if (Boolean.TRUE.equals(message.getIsRecall())) {
            return "已撤回";
        }
        int messageType = message.getMessageType() == null ? MessagePolicy.TYPE_TEXT : message.getMessageType();
        return switch (messageType) {
            case MessagePolicy.TYPE_EMOJI -> "[表情]";
            case MessagePolicy.TYPE_IMAGE -> "[图片]";
            case MessagePolicy.TYPE_FILE -> "[文件]";
            default -> message.getMessageContent();
        };
    }
}
