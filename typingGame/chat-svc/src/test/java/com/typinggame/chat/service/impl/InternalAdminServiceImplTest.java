package com.typinggame.chat.service.impl;

import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.chat.repository.SystemNotificationRepository;
import com.typinggame.chat.repository.UserConversationRepository;
import com.typinggame.chat.repository.UserInfoRepository;
import com.typinggame.common.exception.BusinessException;
import com.typinggame.entity.message.SystemNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalAdminServiceImplTest {

    private SystemNotificationRepository notificationRepository;
    private UserInfoRepository userInfoRepository;
    private InternalAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(SystemNotificationRepository.class);
        userInfoRepository = mock(UserInfoRepository.class);
        service = new InternalAdminServiceImpl(mock(UserConversationRepository.class),
                mock(FriendChatMessageRepository.class), notificationRepository, userInfoRepository);
    }

    @SuppressWarnings("unchecked")
    private List<SystemNotification> savedRows() {
        ArgumentCaptor<List<SystemNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void broadcastCreatesOneUnreadRowPerUser() {
        when(userInfoRepository.findAllUserIds()).thenReturn(List.of(1, 2, 3));

        int count = service.publishNotification(null, " 维护通知 ", "今晚 22 点维护", null);

        assertEquals(3, count);
        List<SystemNotification> rows = savedRows();
        assertEquals(List.of(1, 2, 3), rows.stream().map(SystemNotification::getUserId).toList());
        rows.forEach(n -> {
            assertEquals("维护通知", n.getNotificationTitle());
            assertEquals(InternalAdminServiceImpl.TYPE_ANNOUNCEMENT, n.getNotificationType());
            assertFalse(n.getIsRead());
        });
    }

    @Test
    void targetedNotificationRequiresExistingUser() {
        when(userInfoRepository.existsById(42)).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.publishNotification(42, "t", "c", 1));

        assertEquals(404, e.getCode());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    void rejectsBlankTitle() {
        assertThrows(BusinessException.class, () -> service.publishNotification(null, " ", "c", null));
        verify(notificationRepository, never()).saveAll(any());
    }
}
