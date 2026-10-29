package com.typinggame.user.service.impl;

import com.typinggame.dto.user.UserGameSummaryDTO;
import com.typinggame.dto.user.UserProfileDTO;
import com.typinggame.user.entity.UserInfo;
import com.typinggame.user.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalUserServiceImplTest {

    private UserInfoRepository userInfoRepository;
    private InternalUserServiceImpl service;

    @BeforeEach
    void setUp() {
        userInfoRepository = mock(UserInfoRepository.class);
        service = new InternalUserServiceImpl(userInfoRepository);
    }

    private static UserInfo user(int id, String nickname) {
        UserInfo user = new UserInfo();
        user.setUserId(id);
        user.setUsername("u" + id);
        user.setNickname(nickname);
        user.setAvatar("/avatar/" + id + ".png");
        user.setStatus(UserInfo.STATUS_ACTIVE);
        user.setEnabled(true);
        user.setBestScore(100);
        user.setGameNum(2);
        return user;
    }

    @Test
    void profileMapsPublicFieldsOnly() {
        when(userInfoRepository.findById(1)).thenReturn(Optional.of(user(1, "爱丽丝")));

        UserProfileDTO profile = service.getProfile(1);

        assertEquals(1, profile.getUserId());
        assertEquals("u1", profile.getUsername());
        assertEquals("爱丽丝", profile.getNickname());
        assertEquals("/avatar/1.png", profile.getAvatar());
        assertEquals(100, profile.getBestScore());
        assertEquals(2, profile.getGameNum());
    }

    @Test
    void profileReturnsNullWhenMissingOrIdNull() {
        when(userInfoRepository.findById(9)).thenReturn(Optional.empty());
        assertNull(service.getProfile(9));
        assertNull(service.getProfile(null));
    }

    @Test
    void profilesDedupesAndSkipsAbsentUsers() {
        when(userInfoRepository.findAllById(List.of(1, 2, 3))).thenReturn(List.of(user(1, "一"), user(2, "二")));

        List<UserProfileDTO> profiles = service.getProfiles(List.of(1, 2, 1, 3));

        assertEquals(2, profiles.size());
        assertEquals(1, profiles.get(0).getUserId());
        assertEquals(2, profiles.get(1).getUserId());
    }

    @Test
    void applyGameSummaryKeepsBestWhenScoreLower() {
        UserInfo existing = user(5, "五");
        when(userInfoRepository.findById(5)).thenReturn(Optional.of(existing));
        when(userInfoRepository.save(any(UserInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.applyGameSummary(5, new UserGameSummaryDTO(60, 3, bd("80"), bd("0.7"), bd("120"), bd("0.75")));

        assertEquals(100, existing.getBestScore());
        assertEquals(3, existing.getGameNum());
        assertEquals(bd("80"), existing.getAvgSpeed());
        verify(userInfoRepository).save(existing);
    }

    @Test
    void applyGameSummaryPromotesBestAndAccumulatesGames() {
        UserInfo existing = user(6, "六");
        when(userInfoRepository.findById(6)).thenReturn(Optional.of(existing));
        when(userInfoRepository.save(any(UserInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.applyGameSummary(6, new UserGameSummaryDTO(520, 9, bd("95"), bd("0.88"), bd("140"), bd("0.92")));

        assertEquals(520, existing.getBestScore());
        assertEquals(9, existing.getBestRound());
        assertEquals(bd("140"), existing.getBestSpeed());
        assertEquals(bd("0.92"), existing.getBestAccuracy());
        assertEquals(3, existing.getGameNum());
    }

    @Test
    void existsHandlesNullAndUnknownUsers() {
        assertFalse(service.exists(null));
        when(userInfoRepository.existsById(7)).thenReturn(true);
        assertTrue(service.exists(7));
    }

    @Test
    void applyGameSummaryReturnsNullWhenUserMissing() {
        when(userInfoRepository.findById(8)).thenReturn(Optional.empty());
        assertNull(service.applyGameSummary(8, new UserGameSummaryDTO(1, 1, bd("1"), bd("1"), bd("1"), bd("1"))));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
