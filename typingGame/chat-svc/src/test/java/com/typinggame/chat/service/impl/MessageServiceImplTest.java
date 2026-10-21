package com.typinggame.chat.service.impl;

import com.typinggame.chat.im.ImNotifier;
import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.chat.repository.SystemNotificationRepository;
import com.typinggame.chat.repository.UserConversationRepository;
import com.typinggame.chat.repository.UserInfoRepository;
import com.typinggame.entity.message.SystemNotification;
import com.typinggame.entity.message.UserConversation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock private FriendChatMessageRepository friendChatMessageRepository;
    @Mock private UserConversationRepository userConversationRepository;
    @Mock private UserInfoRepository userInfoRepository;
    @Mock private ImNotifier imNotifier;
    @Mock private SystemNotificationRepository systemNotificationRepository;
    @InjectMocks private MessageServiceImpl service;

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
}
