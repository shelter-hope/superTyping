package com.typinggame.chat.service;

import com.typinggame.chat.im.MessagePolicy;
import com.typinggame.chat.repository.ChatAttachmentRepository;
import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.common.exception.BusinessException;
import com.typinggame.entity.message.ChatAttachment;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatAttachmentServiceTest {

    private final ChatAttachmentRepository repository = mock(ChatAttachmentRepository.class);
    private final FriendChatMessageRepository messages = mock(FriendChatMessageRepository.class);
    private final ChatAttachmentService service = new ChatAttachmentService(repository, messages, "target/chat-files-test");

    @Test
    void strangerCannotDownload() {
        ChatAttachment attachment = imageOwnedBy(1);
        when(repository.findById(3)).thenReturn(Optional.of(attachment));
        when(messages.existsByMessageContentAndMessageTypeAndSenderId("3", MessagePolicy.TYPE_IMAGE, 9)).thenReturn(false);
        when(messages.existsByMessageContentAndMessageTypeAndReceiverId("3", MessagePolicy.TYPE_IMAGE, 9)).thenReturn(false);

        BusinessException error = assertThrows(BusinessException.class, () -> service.loadFor(9, 3));
        assertEquals(403, error.getCode());
    }

    @Test
    void participantPassesTheAcl() {
        ChatAttachment attachment = imageOwnedBy(1);
        when(repository.findById(3)).thenReturn(Optional.of(attachment));
        when(messages.existsByMessageContentAndMessageTypeAndReceiverId("3", MessagePolicy.TYPE_IMAGE, 5)).thenReturn(true);

        BusinessException missingFile = assertThrows(BusinessException.class, () -> service.loadFor(5, 3));
        assertEquals(404, missingFile.getCode());
    }

    @Test
    void sendRequiresOwnerAndMatchingKind() {
        ChatAttachment attachment = imageOwnedBy(1);
        when(repository.findById(3)).thenReturn(Optional.of(attachment));

        assertEquals(3, service.requireForSend(1, "3", MessagePolicy.TYPE_IMAGE).getId());
        assertThrows(IllegalArgumentException.class, () -> service.requireForSend(5, "3", MessagePolicy.TYPE_IMAGE));
        assertThrows(IllegalArgumentException.class, () -> service.requireForSend(1, "3", MessagePolicy.TYPE_FILE));
    }

    private static ChatAttachment imageOwnedBy(int ownerId) {
        ChatAttachment attachment = new ChatAttachment();
        attachment.setId(3);
        attachment.setOwnerId(ownerId);
        attachment.setKind(MessagePolicy.TYPE_IMAGE);
        attachment.setStoredName("missing.png");
        attachment.setOriginalName("a.png");
        attachment.setMediaType("image/png");
        attachment.setSizeBytes(10L);
        return attachment;
    }
}
