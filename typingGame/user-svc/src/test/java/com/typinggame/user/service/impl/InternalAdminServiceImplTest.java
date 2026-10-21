package com.typinggame.user.service.impl;

import com.typinggame.common.exception.BusinessException;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.user.repository.FriendRelationRepository;
import com.typinggame.user.repository.UserAppConfigRepository;
import com.typinggame.user.repository.UserDailyStatsRepository;
import com.typinggame.user.repository.UserGameModeConfigRepository;
import com.typinggame.user.repository.UserInfoRepository;
import com.typinggame.user.repository.UserInfoSnapshotRepository;
import com.typinggame.user.repository.UserModelParamsRepository;
import com.typinggame.user.repository.UserMonsterConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalAdminServiceImplTest {

    private UserInfoRepository userInfoRepository;
    private InternalAdminServiceImpl service;
    private UserInfo user;

    @BeforeEach
    void setUp() {
        userInfoRepository = mock(UserInfoRepository.class);
        service = new InternalAdminServiceImpl(userInfoRepository,
                mock(FriendRelationRepository.class), mock(UserAppConfigRepository.class),
                mock(UserGameModeConfigRepository.class), mock(UserMonsterConfigRepository.class),
                mock(UserModelParamsRepository.class), mock(UserDailyStatsRepository.class),
                mock(UserInfoSnapshotRepository.class));
        user = new UserInfo();
        user.setUserId(9);
        user.setRole("user");
        user.setNickname("old");
        when(userInfoRepository.findById(9)).thenReturn(Optional.of(user));
    }

    @Test
    void disablesRegularUser() {
        service.updateUserStatus(9, UserInfo.STATUS_DISABLED);

        assertEquals(UserInfo.STATUS_DISABLED, user.getStatus());
        verify(userInfoRepository).save(user);
    }

    @Test
    void refusesToDisableAdmin() {
        user.setRole("admin");

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.updateUserStatus(9, UserInfo.STATUS_DISABLED));

        assertEquals(400, e.getCode());
        verify(userInfoRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownStatus() {
        assertThrows(BusinessException.class, () -> service.updateUserStatus(9, 2));
    }

    @Test
    void updatesProfileOfTargetUser() {
        service.updateUserProfile(9, "  new  ", "hello");

        assertEquals("new", user.getNickname());
        assertEquals("hello", user.getSignature());
        verify(userInfoRepository).save(user);
    }

    @Test
    void rejectsBlankNickname() {
        assertThrows(BusinessException.class, () -> service.updateUserProfile(9, "   ", null));
        assertEquals("old", user.getNickname());
    }
}
