package com.typinggame.service.impl;

import com.typinggame.dto.message.MessageDTO;
import com.typinggame.entity.message.FriendChatMessage;
import com.typinggame.entity.message.UserConversation;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.enums.ReadStatus;
import com.typinggame.repository.FriendChatMessageRepository;
import com.typinggame.repository.UserConversationRepository;
import com.typinggame.repository.UserInfoRepository;
import com.typinggame.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    private static final int TYPE_FRIEND = 1;
    private static final int TYPE_SYSTEM = 2;

    @Override
    public List<MessageDTO> getConversations(Integer userId) {
        List<UserConversation> conversations = userConversationRepository.findByUserId(userId);
        return conversations.stream().map(conversation -> {
            Optional<UserInfo> otherUserOptional = userInfoRepository.findById(conversation.getTargetUserId());
            if (otherUserOptional.isPresent()) {
                UserInfo otherUser = otherUserOptional.get();
                MessageDTO messageDTO = new MessageDTO();
                messageDTO.setId(conversation.getLastMessageId()); // Use last message ID or null
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
                messageDTO.setType("friend"); // Normalized type string for frontend
                return messageDTO;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getConversationMessages(Integer conversationId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        List<FriendChatMessage> messages = friendChatMessageRepository.findByConversationIdOrderBySendTimeDesc(conversationId, pageable);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getMessages(Integer userId, Integer friendId, Integer offset) {
        // 通过当前用户与好友的会话关系找到会话ID
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
    public MessageDTO sendMessage(Integer senderId, Integer receiverId, String content, String type) {
        int msgType = "system".equalsIgnoreCase(type) ? TYPE_SYSTEM : TYPE_FRIEND;
        
        // 保存消息到数据库
        FriendChatMessage message = new FriendChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setMessageContent(content);
        message.setMessageType(msgType);
        message.setReadStatus(ReadStatus.UNREAD.getCode());
        message.setSendTime(LocalDateTime.now());
        // Need to set conversationId. 
        // Logic: Find existing conversation or create new?
        // Usually message belongs to a conversation. 
        // If system message, maybe no conversation or special one?
        // Let's assume system messages don't need conversation ID for now or use 0?
        // FriendChatMessage has conversation_id NOT NULL.
        // So we MUST create conversation first.
        
        UserConversation senderConv = getOrCreateConversation(senderId, receiverId);
        // Note: receiver might also need conversation created if we want dual copy or shared ID.
        // Current impl seems to use dual conversations (UserConversation per user).
        // But FriendChatMessage refers to 'conversation_id'. Which one?
        // Usually chat messages are shared, linked to a unique "ChatSession" or similar.
        // But here FriendChatMessage has conversationId. 
        // If UserConversation is per user, then we have 2 conversation IDs for same pair?
        // If FriendChatMessage is shared, it should link to a shared session ID.
        // Looking at getConversations, it uses UserConversation.
        // Looking at getConversationMessages, it uses FriendChatMessage.conversationId.
        // This implies FriendChatMessage is linked to ONE conversation ID.
        // If UserConversation is per user, we have a problem if FriendChatMessage links to only one.
        // Unless UserConversation (sender) and UserConversation (receiver) share same ID? No, ID is auto-inc.
        
        // Let's assume for now we link to SENDER's conversation ID for simplicity, 
        // OR we have to duplicate messages?
        // Duplicate messages is common in some architectures (inbox/outbox).
        // OR we link to a "SessionId" which is missing in current schema.
        // Let's use senderConv.getId() for now.
        
        message.setConversationId(senderConv.getId());
        friendChatMessageRepository.save(message);

        // Update sender conversation
        updateConversation(senderConv, message);

        // Update receiver conversation
        UserConversation receiverConv = getOrCreateConversation(receiverId, senderId);
        updateConversation(receiverConv, message);
        // Receiver's conversation ID is different. 
        // If FriendChatMessage links to senderConv.getId(), receiver won't find it if searching by receiverConv.getId().
        // THIS IS A BUG in current schema/logic.
        // Fix: Either duplicate message for receiver, or introduce shared Session.
        // Given I can't change schema easily right now without migration script, 
        // I will DUPLICATE the message for receiver with receiver's conversation ID.
        
        FriendChatMessage receiverMessage = new FriendChatMessage();
        receiverMessage.setSenderId(senderId);
        receiverMessage.setReceiverId(receiverId);
        receiverMessage.setMessageContent(content);
        receiverMessage.setMessageType(msgType);
        receiverMessage.setReadStatus(ReadStatus.UNREAD.getCode());
        receiverMessage.setSendTime(LocalDateTime.now());
        receiverMessage.setConversationId(receiverConv.getId());
        friendChatMessageRepository.save(receiverMessage);

        return convertToDTO(message);
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
        // If message is incoming (userId == receiverId), increment unread
        if (conversation.getUserId().equals(message.getReceiverId())) {
             conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        }
        userConversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public boolean markMessageAsRead(Integer messageId) {
        Optional<FriendChatMessage> messageOptional = friendChatMessageRepository.findById(messageId);
        if (messageOptional.isPresent()) {
            FriendChatMessage message = messageOptional.get();
            if (!Objects.equals(message.getReadStatus(), ReadStatus.READ.getCode())) {
                message.setReadStatus(ReadStatus.READ.getCode());
                friendChatMessageRepository.save(message);
                
                // Decrement unread count in conversation
                Optional<UserConversation> convOpt = userConversationRepository.findById(message.getConversationId());
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
        // 查出当前用户收到的所有未读消息
        List<FriendChatMessage> unreadMessages =
                friendChatMessageRepository.findByReceiverIdAndReadStatus(userId, ReadStatus.UNREAD.getCode());
        if (unreadMessages.isEmpty()) {
            return;
        }

        // 先按会话分组，用于后续刷新未读计数
        var byConversation = unreadMessages.stream()
                .collect(Collectors.groupingBy(FriendChatMessage::getConversationId));

        // 将消息标记为已读
        LocalDateTime now = LocalDateTime.now();
        for (FriendChatMessage message : unreadMessages) {
            message.setReadStatus(ReadStatus.READ.getCode());
            message.setReadTime(now);
        }
        friendChatMessageRepository.saveAll(unreadMessages);

        // 刷新每个会话的未读数量（以数据库为准重新统计）
        for (Integer conversationId : byConversation.keySet()) {
            Optional<UserConversation> convOpt = userConversationRepository.findById(conversationId);
            if (convOpt.isPresent()) {
                UserConversation conv = convOpt.get();
                // 只更新当前用户所属的会话
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
        // 用户进入与某好友的聊天界面时，将该会话内当前用户收到的未读消息全部置为已读（read_status=1）
        Optional<UserConversation> convOpt = userConversationRepository.findByUserIdAndTargetUserId(userId, friendId);
        if (!convOpt.isPresent()) {
            return;
        }
        Integer conversationId = convOpt.get().getId();
        List<FriendChatMessage> unread = friendChatMessageRepository.findByReceiverIdAndReadStatus(userId, ReadStatus.UNREAD.getCode()).stream()
                .filter(m -> conversationId.equals(m.getConversationId()))
                .collect(Collectors.toList());

        // 若当前会话下存在未读消息，则全部置为已读
        if (!unread.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (FriendChatMessage m : unread) {
                m.setReadStatus(ReadStatus.READ.getCode()); // 1=已读
                m.setReadTime(now);
            }
            friendChatMessageRepository.saveAll(unread);
        }

        // 无论是否存在未读消息，都以数据库为准重新统计并刷新会话未读数
        int unreadCount = friendChatMessageRepository
                .countByConversationIdAndReceiverIdAndReadStatus(conversationId, userId, ReadStatus.UNREAD.getCode());
        UserConversation conv = convOpt.get();
        conv.setUnreadCount(unreadCount);
        userConversationRepository.save(conv);
    }

    @Override
    public boolean deleteMessage(Integer messageId) {
        if (friendChatMessageRepository.existsById(messageId)) {
            friendChatMessageRepository.deleteById(messageId);
            return true;
        }
        return false;
    }

    @Override
    public Integer getUnreadCount(Integer userId) {
        return friendChatMessageRepository.countByReceiverIdAndReadStatus(userId, ReadStatus.UNREAD.getCode());
    }

    @Override
    public List<MessageDTO> getSystemMessages(Integer userId) {
        List<FriendChatMessage> messages = friendChatMessageRepository.findByReceiverIdAndMessageTypeOrderBySendTimeDesc(userId, TYPE_SYSTEM);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private MessageDTO convertToDTO(FriendChatMessage message) {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(message.getId());
        messageDTO.setConversationId(message.getConversationId());
        messageDTO.setSenderId(message.getSenderId());
        
        // Populate names/avatars
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
        return messageDTO;
    }
}
