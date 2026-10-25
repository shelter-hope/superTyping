package com.typinggame.chat.application;

import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.chat.repository.UserConversationRepository;
import com.typinggame.chat.repository.UserInfoRepository;
import com.typinggame.dto.message.MessageDTO;
import com.typinggame.entity.message.FriendChatMessage;
import com.typinggame.entity.message.UserConversation;
import com.typinggame.entity.user.UserInfo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConversationQueryService {
    private final UserConversationRepository userConversationRepository;
    private final FriendChatMessageRepository friendChatMessageRepository;
    private final UserInfoRepository userInfoRepository;
    private final MessageDtoMapper messageDtoMapper;

    public ConversationQueryService(UserConversationRepository userConversationRepository,
                                    FriendChatMessageRepository friendChatMessageRepository,
                                    UserInfoRepository userInfoRepository,
                                    MessageDtoMapper messageDtoMapper) {
        this.userConversationRepository = userConversationRepository;
        this.friendChatMessageRepository = friendChatMessageRepository;
        this.userInfoRepository = userInfoRepository;
        this.messageDtoMapper = messageDtoMapper;
    }

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

    public List<MessageDTO> getConversationMessages(Integer userId, Integer conversationId, Integer page, Integer size) {
        if (userConversationRepository.findByIdAndUserId(conversationId, userId).isEmpty()) {
            return new ArrayList<>();
        }
        Pageable pageable = PageRequest.of(page, size);
        List<FriendChatMessage> messages =
                friendChatMessageRepository.findByConversationIdOrderBySendTimeDesc(conversationId, pageable);
        return messages.stream().map(messageDtoMapper::toDto).collect(Collectors.toList());
    }

    public List<MessageDTO> getMessages(Integer userId, Integer friendId, Integer offset) {
        Optional<UserConversation> conversationOpt =
                userConversationRepository.findByUserIdAndTargetUserId(userId, friendId);
        if (conversationOpt.isEmpty()) {
            return new ArrayList<>();
        }
        Integer conversationId = conversationOpt.get().getId();
        int page = offset != null && offset >= 0 ? offset : 0;
        Pageable pageable = PageRequest.of(page, 20);
        List<FriendChatMessage> messages =
                friendChatMessageRepository.findByConversationIdOrderBySendTimeDesc(conversationId, pageable);
        return messages.stream().map(messageDtoMapper::toDto).collect(Collectors.toList());
    }

    public List<MessageDTO> getMessagesBefore(Integer userId, Integer friendId, Integer beforeId, int limit) {
        Optional<UserConversation> conversationOpt =
                userConversationRepository.findByUserIdAndTargetUserId(userId, friendId);
        if (conversationOpt.isEmpty()) {
            return List.of();
        }
        int size = Math.max(1, Math.min(50, limit));
        List<FriendChatMessage> messages = beforeId == null
                ? friendChatMessageRepository.findByConversationIdOrderBySendTimeDesc(
                conversationOpt.get().getId(), PageRequest.of(0, size))
                : friendChatMessageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(
                conversationOpt.get().getId(), beforeId, PageRequest.of(0, size));
        return messages.stream().map(messageDtoMapper::toDto).collect(Collectors.toList());
    }
}
