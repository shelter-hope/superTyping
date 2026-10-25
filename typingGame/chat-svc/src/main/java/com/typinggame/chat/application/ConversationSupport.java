package com.typinggame.chat.application;

import com.typinggame.chat.repository.UserConversationRepository;
import com.typinggame.entity.message.FriendChatMessage;
import com.typinggame.entity.message.UserConversation;
import org.springframework.stereotype.Component;

@Component
public class ConversationSupport {
    private final UserConversationRepository userConversationRepository;

    public ConversationSupport(UserConversationRepository userConversationRepository) {
        this.userConversationRepository = userConversationRepository;
    }

    public UserConversation getOrCreate(Integer userId, Integer targetUserId) {
        return userConversationRepository.findByUserIdAndTargetUserId(userId, targetUserId)
                .orElseGet(() -> {
                    UserConversation conversation = new UserConversation();
                    conversation.setUserId(userId);
                    conversation.setTargetUserId(targetUserId);
                    conversation.setUnreadCount(0);
                    userConversationRepository.save(conversation);
                    return conversation;
                });
    }

    public void updateLastMessage(UserConversation conversation, FriendChatMessage message) {
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageContent(MessageDtoMapper.preview(message));
        conversation.setLastMessageTime(message.getSendTime());
        if (conversation.getUserId().equals(message.getReceiverId())) {
            conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        }
        userConversationRepository.save(conversation);
    }
}
