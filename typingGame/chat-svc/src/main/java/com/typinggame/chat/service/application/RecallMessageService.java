package com.typinggame.chat.service.application;

import com.typinggame.chat.support.im.ImEventBus;
import com.typinggame.chat.support.im.MessagePolicy;
import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.chat.repository.UserConversationRepository;
import com.typinggame.common.exception.BusinessException;
import com.typinggame.dto.message.MessageDTO;
import com.typinggame.chat.entity.FriendChatMessage;
import com.typinggame.chat.entity.UserConversation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecallMessageService {
    private final FriendChatMessageRepository friendChatMessageRepository;
    private final UserConversationRepository userConversationRepository;
    private final MessageDtoMapper messageDtoMapper;
    private final ImEventBus imEventBus;

    public RecallMessageService(FriendChatMessageRepository friendChatMessageRepository,
                                UserConversationRepository userConversationRepository,
                                MessageDtoMapper messageDtoMapper,
                                ImEventBus imEventBus) {
        this.friendChatMessageRepository = friendChatMessageRepository;
        this.userConversationRepository = userConversationRepository;
        this.messageDtoMapper = messageDtoMapper;
        this.imEventBus = imEventBus;
    }

    @Transactional
    public MessageDTO recall(Integer userId, Integer messageId) {
        FriendChatMessage message = friendChatMessageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("消息不存在"));
        if (!userId.equals(message.getSenderId())) {
            throw BusinessException.forbidden("只能撤回自己的消息");
        }
        if (Boolean.TRUE.equals(message.getIsRecall())) {
            return messageDtoMapper.toDto(message);
        }
        LocalDateTime sentAt = message.getSendTime();
        if (sentAt == null || LocalDateTime.now().isAfter(sentAt.plusMinutes(MessagePolicy.RECALL_MINUTES))) {
            throw BusinessException.badRequest("已超过撤回时间");
        }
        List<FriendChatMessage> rows = peerRows(message);
        LocalDateTime now = LocalDateTime.now();
        for (FriendChatMessage row : rows) {
            row.setIsRecall(true);
            row.setRecallTime(now);
        }
        friendChatMessageRepository.saveAll(rows);
        Set<Integer> recalledIds = rows.stream().map(FriendChatMessage::getId).collect(Collectors.toSet());
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
        imEventBus.pushRecall(List.of(message.getSenderId(), message.getReceiverId()), message.getClientMsgId());
        return messageDtoMapper.toDto(message);
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
}
