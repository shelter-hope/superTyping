package com.typinggame.chat.service.impl;

import com.typinggame.chat.application.ConversationQueryService;
import com.typinggame.chat.application.ConversationSupport;
import com.typinggame.chat.application.MessageDtoMapper;
import com.typinggame.chat.application.MessageReadService;
import com.typinggame.chat.application.NotificationQueryService;
import com.typinggame.chat.application.RecallMessageService;
import com.typinggame.chat.application.SendMessageService;
import com.typinggame.chat.friends.CanMessageGuard;
import com.typinggame.chat.friends.JpaCanMessageAdapter;
import com.typinggame.chat.friends.UserSvcCanMessageAdapter;
import com.typinggame.chat.im.ImEventBus;
import com.typinggame.chat.im.MessagePolicy;
import com.typinggame.chat.repository.ChatAttachmentRepository;
import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.chat.repository.FriendRelationRepository;
import com.typinggame.chat.repository.SystemNotificationRepository;
import com.typinggame.chat.repository.UserConversationRepository;
import com.typinggame.chat.repository.UserInfoRepository;
import com.typinggame.chat.service.ChatAttachmentService;
import com.typinggame.common.exception.BusinessException;
import com.typinggame.dto.message.MessageDTO;
import com.typinggame.entity.message.ChatAttachment;
import com.typinggame.entity.message.FriendChatMessage;
import com.typinggame.entity.message.FriendRelation;
import com.typinggame.entity.message.SystemNotification;
import com.typinggame.entity.message.UserConversation;
import com.typinggame.enums.ReadStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock private FriendChatMessageRepository friendChatMessageRepository;
    @Mock private UserConversationRepository userConversationRepository;
    @Mock private UserInfoRepository userInfoRepository;
    @Mock private FriendRelationRepository friendRelationRepository;
    @Mock private ImEventBus imEventBus;
    @Mock private SystemNotificationRepository systemNotificationRepository;
    @Mock private ChatAttachmentService chatAttachmentService;
    @Mock private ChatAttachmentRepository chatAttachmentRepository;

    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        MessageDtoMapper mapper = new MessageDtoMapper(userInfoRepository, chatAttachmentRepository);
        ConversationSupport conversationSupport = new ConversationSupport(userConversationRepository);
        NotificationQueryService notificationQueryService = new NotificationQueryService(
                friendChatMessageRepository, systemNotificationRepository, userConversationRepository, mapper);
        CanMessageGuard canMessageGuard = new CanMessageGuard(
                new JpaCanMessageAdapter(friendRelationRepository),
                mock(UserSvcCanMessageAdapter.class),
                "local");
        SendMessageService sendMessageService = new SendMessageService(
                friendChatMessageRepository, canMessageGuard, chatAttachmentService,
                conversationSupport, mapper, imEventBus);
        RecallMessageService recallMessageService = new RecallMessageService(
                friendChatMessageRepository, userConversationRepository, mapper, imEventBus);
        ConversationQueryService conversationQueryService = new ConversationQueryService(
                userConversationRepository, friendChatMessageRepository, userInfoRepository, mapper);
        MessageReadService messageReadService = new MessageReadService(
                friendChatMessageRepository, userConversationRepository, imEventBus);
        service = new MessageServiceImpl(
                sendMessageService, recallMessageService, conversationQueryService,
                messageReadService, notificationQueryService);
    }

    private static UserConversation conversation(Integer unread) {
        UserConversation c = new UserConversation();
        c.setUnreadCount(unread);
        return c;
    }

    @Test
    void unreadCountSumsConversationsAndNotifications() {
        when(userConversationRepository.findByUserId(7))
                .thenReturn(List.of(conversation(2), conversation(null), conversation(3)));
        when(systemNotificationRepository.countByUserIdAndIsRead(7, false)).thenReturn(4L);

        assertEquals(9, service.getUnreadCount(7));
        verify(friendChatMessageRepository, never()).findAll();
    }

    @Test
    void marksOwnNotificationAsRead() {
        SystemNotification n = new SystemNotification();
        n.setIsRead(false);
        when(systemNotificationRepository.findByIdAndUserId(11, 7)).thenReturn(Optional.of(n));

        assertTrue(service.markNotificationAsRead(7, 11));
        assertTrue(n.getIsRead());
        verify(systemNotificationRepository).save(n);
    }

    @Test
    void cannotMarkSomeoneElsesNotification() {
        when(systemNotificationRepository.findByIdAndUserId(11, 8)).thenReturn(Optional.empty());

        assertFalse(service.markNotificationAsRead(8, 11));
        verify(systemNotificationRepository, never()).save(any());
    }

    @Test
    void textEmojiAndImageReuseClientMsgId() {
        when(friendChatMessageRepository.findFirstBySenderIdAndClientMsgId(3, "text-1"))
                .thenReturn(Optional.of(stored(21, MessagePolicy.TYPE_TEXT, "hi")));
        when(friendChatMessageRepository.findFirstBySenderIdAndClientMsgId(3, "emoji-1"))
                .thenReturn(Optional.of(stored(22, MessagePolicy.TYPE_EMOJI, "smile")));
        when(friendChatMessageRepository.findFirstBySenderIdAndClientMsgId(3, "image-1"))
                .thenReturn(Optional.of(stored(23, MessagePolicy.TYPE_IMAGE, "4")));
        ChatAttachment attachment = new ChatAttachment();
        attachment.setOriginalName("a.png");
        attachment.setMediaType("image/png");
        attachment.setSizeBytes(12L);
        when(chatAttachmentRepository.findById(4)).thenReturn(Optional.of(attachment));
        when(userInfoRepository.findById(any())).thenReturn(Optional.empty());

        assertEquals(21, service.sendMessage(3, 5, "hi", "text", "text-1").getId());
        assertEquals("emoji", service.sendMessage(3, 5, "smile", "emoji", "emoji-1").getType());
        MessageDTO image = service.sendMessage(3, 5, "4", "image", "image-1");
        assertEquals(23, image.getId());
        assertEquals("a.png", image.getAttachmentName());
        verify(friendChatMessageRepository, never()).save(any());
        verify(chatAttachmentService, never()).requireForSend(any(), any(), anyInt());
    }

    @Test
    void nonFriendAndBlockedCannotSend() {
        when(friendChatMessageRepository.findFirstBySenderIdAndClientMsgId(3, "c")).thenReturn(Optional.empty());
        when(friendRelationRepository.findAccepted(3, 5)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.sendMessage(3, 5, "hi", "text", "c"));
    }

    @Test
    void storesResolvedTypeOnBothRows() {
        stubAcceptedPair();
        ChatAttachment attachment = new ChatAttachment();
        attachment.setId(4);
        when(chatAttachmentService.requireForSend(3, "4", MessagePolicy.TYPE_IMAGE)).thenReturn(attachment);

        service.sendMessage(3, 5, "smile", "emoji", "e1");
        service.sendMessage(3, 5, "4", "image", "i1");

        ArgumentCaptor<FriendChatMessage> saved = ArgumentCaptor.forClass(FriendChatMessage.class);
        verify(friendChatMessageRepository, times(4)).save(saved.capture());
        assertEquals(MessagePolicy.TYPE_EMOJI, saved.getAllValues().get(0).getMessageType());
        assertEquals("smile", saved.getAllValues().get(0).getMessageContent());
        assertEquals(MessagePolicy.TYPE_EMOJI, saved.getAllValues().get(1).getMessageType());
        assertEquals(MessagePolicy.TYPE_IMAGE, saved.getAllValues().get(2).getMessageType());
        assertEquals("4", saved.getAllValues().get(2).getMessageContent());
    }

    @Test
    void recallMarksBothRowsAndRejectsTimeout() {
        FriendChatMessage senderRow = stored(11, MessagePolicy.TYPE_TEXT, "hi");
        senderRow.setClientMsgId("c1");
        senderRow.setSendTime(LocalDateTime.now());
        FriendChatMessage receiverRow = stored(12, MessagePolicy.TYPE_TEXT, "hi");
        receiverRow.setClientMsgId("c1");
        when(friendChatMessageRepository.findById(11)).thenReturn(Optional.of(senderRow));
        when(friendChatMessageRepository.findBySenderIdAndClientMsgId(3, "c1")).thenReturn(List.of(senderRow, receiverRow));
        when(userInfoRepository.findById(any())).thenReturn(Optional.empty());
        when(userConversationRepository.findByUserIdAndTargetUserId(any(), any())).thenReturn(Optional.empty());

        service.recall(3, 11);

        assertTrue(senderRow.getIsRecall());
        assertTrue(receiverRow.getIsRecall());
        verify(imEventBus).pushRecall(eq(List.of(3, 5)), eq("c1"));

        senderRow.setIsRecall(false);
        senderRow.setSendTime(LocalDateTime.now().minusMinutes(3));
        BusinessException timeout = assertThrows(BusinessException.class, () -> service.recall(3, 11));
        assertEquals(400, timeout.getCode());
    }

    @Test
    void markAsReadUpdatesRowsOnBothConversations() {
        UserConversation mine = conversation(2);
        mine.setId(10);
        mine.setUserId(5);
        UserConversation theirs = conversation(0);
        theirs.setId(11);
        FriendChatMessage onMine = stored(1, MessagePolicy.TYPE_TEXT, "a");
        onMine.setConversationId(10);
        onMine.setReadStatus(ReadStatus.UNREAD.getCode());
        FriendChatMessage onTheirs = stored(2, MessagePolicy.TYPE_TEXT, "a");
        onTheirs.setConversationId(11);
        onTheirs.setReadStatus(ReadStatus.UNREAD.getCode());
        FriendChatMessage elsewhere = stored(3, MessagePolicy.TYPE_TEXT, "b");
        elsewhere.setConversationId(99);
        elsewhere.setReadStatus(ReadStatus.UNREAD.getCode());
        when(userConversationRepository.findByUserIdAndTargetUserId(5, 3)).thenReturn(Optional.of(mine));
        when(userConversationRepository.findByUserIdAndTargetUserId(3, 5)).thenReturn(Optional.of(theirs));
        when(friendChatMessageRepository.findByReceiverIdAndReadStatus(5, ReadStatus.UNREAD.getCode()))
                .thenReturn(List.of(onMine, onTheirs, elsewhere));
        when(friendChatMessageRepository.countByConversationIdAndReceiverIdAndReadStatus(10, 5, ReadStatus.UNREAD.getCode()))
                .thenReturn(0);
        when(userConversationRepository.findByUserId(5)).thenReturn(List.of(mine));
        when(systemNotificationRepository.countByUserIdAndIsRead(5, false)).thenReturn(0L);

        service.markAsReadByFriend(5, 3);

        assertEquals(ReadStatus.READ.getCode(), onMine.getReadStatus());
        assertEquals(ReadStatus.READ.getCode(), onTheirs.getReadStatus());
        assertEquals(ReadStatus.UNREAD.getCode(), elsewhere.getReadStatus());
        verify(imEventBus).pushRead(3, 5);
    }

    private void stubAcceptedPair() {
        UserConversation sender = conversation(0);
        sender.setId(10);
        sender.setUserId(3);
        sender.setTargetUserId(5);
        UserConversation receiver = conversation(0);
        receiver.setId(11);
        receiver.setUserId(5);
        receiver.setTargetUserId(3);
        when(friendChatMessageRepository.findFirstBySenderIdAndClientMsgId(eq(3), any())).thenReturn(Optional.empty());
        when(friendRelationRepository.findAccepted(3, 5)).thenReturn(Optional.of(new FriendRelation()));
        when(userConversationRepository.findByUserIdAndTargetUserId(3, 5)).thenReturn(Optional.of(sender));
        when(userConversationRepository.findByUserIdAndTargetUserId(5, 3)).thenReturn(Optional.of(receiver));
        when(friendChatMessageRepository.maxServerSeq(any())).thenReturn(0);
        when(friendChatMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userInfoRepository.findById(any())).thenReturn(Optional.empty());
        when(userConversationRepository.findByUserId(5)).thenReturn(List.of(receiver));
        when(systemNotificationRepository.countByUserIdAndIsRead(5, false)).thenReturn(0L);
    }

    private static FriendChatMessage stored(int id, int type, String content) {
        FriendChatMessage message = new FriendChatMessage();
        message.setId(id);
        message.setSenderId(3);
        message.setReceiverId(5);
        message.setMessageType(type);
        message.setMessageContent(content);
        message.setReadStatus(ReadStatus.UNREAD.getCode());
        message.setSendTime(LocalDateTime.now());
        return message;
    }
}
