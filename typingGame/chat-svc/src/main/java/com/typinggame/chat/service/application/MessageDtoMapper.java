package com.typinggame.chat.service.application;

import com.typinggame.chat.support.im.MessagePolicy;
import com.typinggame.chat.repository.ChatAttachmentRepository;
import com.typinggame.chat.support.UserProfileClient;
import com.typinggame.dto.message.MessageDTO;
import com.typinggame.dto.user.UserProfileDTO;
import com.typinggame.chat.entity.ChatAttachment;
import com.typinggame.chat.entity.FriendChatMessage;
import com.typinggame.chat.entity.SystemNotification;
import com.typinggame.enums.ReadStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class MessageDtoMapper {
    static final String NOTIFICATION_TYPE = "notification";

    private final UserProfileClient userProfileClient;
    private final ChatAttachmentRepository chatAttachmentRepository;

    public MessageDtoMapper(UserProfileClient userProfileClient,
                            ChatAttachmentRepository chatAttachmentRepository) {
        this.userProfileClient = userProfileClient;
        this.chatAttachmentRepository = chatAttachmentRepository;
    }

    public MessageDTO toDto(FriendChatMessage message) {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(message.getId());
        messageDTO.setConversationId(message.getConversationId());
        messageDTO.setSenderId(message.getSenderId());
        messageDTO.setReceiverId(message.getReceiverId());
        Map<Integer, UserProfileDTO> profiles = userProfileClient.fetchProfiles(userIdsOf(message));
        UserProfileDTO sender = message.getSenderId() == null ? null : profiles.get(message.getSenderId());
        if (sender != null) {
            messageDTO.setSenderName(displayName(sender));
            messageDTO.setSenderAvatar(sender.getAvatar());
        }
        UserProfileDTO receiver = message.getReceiverId() == null ? null : profiles.get(message.getReceiverId());
        if (receiver != null) {
            messageDTO.setReceiverName(displayName(receiver));
        }
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

    public MessageDTO notificationToDto(SystemNotification n) {
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

    public static String preview(FriendChatMessage message) {
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

    public static String normalizeContent(int messageType, String content) {
        MessagePolicy.requireContent(content);
        String value = content.trim();
        if (messageType == MessagePolicy.TYPE_EMOJI) {
            MessagePolicy.requireEmoji(value);
            return value;
        }
        return content;
    }

    private static List<Integer> userIdsOf(FriendChatMessage message) {
        return Stream.of(message.getSenderId(), message.getReceiverId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static String displayName(UserProfileDTO profile) {
        String nickname = profile.getNickname();
        return nickname == null || nickname.isBlank() ? profile.getUsername() : nickname;
    }

    private Optional<ChatAttachment> attachmentOf(String content) {
        if (content == null) {
            return Optional.empty();
        }
        try {
            return chatAttachmentRepository.findById(Integer.parseInt(content.trim()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
