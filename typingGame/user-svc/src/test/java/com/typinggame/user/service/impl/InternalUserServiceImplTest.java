package com.typinggame.user.service.impl;

import com.typinggame.dto.user.DailyStatsUpdateDTO;
import com.typinggame.dto.user.UserGameSummaryDTO;
import com.typinggame.dto.user.UserProfileDTO;
import com.typinggame.user.entity.UserDailyStats;
import com.typinggame.user.entity.UserInfo;
import com.typinggame.user.repository.UserDailyStatsRepository;
import com.typinggame.user.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private UserDailyStatsRepository userDailyStatsRepository;
    private InternalUserServiceImpl service;

    @BeforeEach
    void setUp() {
        userInfoRepository = mock(UserInfoRepository.class);
        userDailyStatsRepository = mock(UserDailyStatsRepository.class);
        service = new InternalUserServiceImpl(userInfoRepository, userDailyStatsRepository);
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

    @Test
    void upsertDailyStatsCreatesRowForNewDay() {
        LocalDate day = LocalDate.of(2026, 10, 30);
        when(userDailyStatsRepository.findByUserIdAndStatDate(3, day)).thenReturn(Optional.empty());
        when(userDailyStatsRepository.save(any(UserDailyStats.class))).thenAnswer(inv -> inv.getArgument(0));

        service.upsertDailyStats(3, update(day, 120, 100, 80, "92.5", "0.86", "110", "0.9"));

        ArgumentCaptor<UserDailyStats> captor = ArgumentCaptor.forClass(UserDailyStats.class);
        verify(userDailyStatsRepository).save(captor.capture());
        UserDailyStats saved = captor.getValue();
        assertEquals(3, saved.getUserId());
        assertEquals(day, saved.getStatDate());
        assertEquals(1, saved.getGameCount());
        assertEquals(120, saved.getTotalScore());
        assertEquals(bd("92.50"), saved.getAvgSpeed());
        assertEquals(bd("0.86"), saved.getAvgAccuracy());
        assertEquals("ENDLESS", saved.getGameMode());
    }

    @Test
    void upsertDailyStatsAccumulatesAndKeepsPeaks() {
        LocalDate day = LocalDate.of(2026, 10, 30);
        UserDailyStats existing = new UserDailyStats();
        existing.setUserId(4);
        existing.setStatDate(day);
        existing.setGameCount(1);
        existing.setTotalScore(200);
        existing.setTotalChars(150);
        existing.setCorrectChars(120);
        existing.setAvgSpeed(bd("100"));
        existing.setAvgAccuracy(bd("0.80"));
        existing.setPeakSpeed(bd("105"));
        existing.setPeakAccuracy(bd("0.82"));
        when(userDailyStatsRepository.findByUserIdAndStatDate(4, day)).thenReturn(Optional.of(existing));
        when(userDailyStatsRepository.save(any(UserDailyStats.class))).thenAnswer(inv -> inv.getArgument(0));

        service.upsertDailyStats(4, update(day, 100, 60, 50, "120", "0.90", "100", "0.88"));

        assertEquals(2, existing.getGameCount());
        assertEquals(300, existing.getTotalScore());
        assertEquals(210, existing.getTotalChars());
        assertEquals(170, existing.getCorrectChars());
        // 加权：(100*1 + 120) / 2 = 110
        assertEquals(bd("110.00"), existing.getAvgSpeed());
        assertEquals(bd("0.85"), existing.getAvgAccuracy());
        assertEquals(bd("105"), existing.getPeakSpeed());
        assertEquals(bd("0.88"), existing.getPeakAccuracy());
    }

    private static DailyStatsUpdateDTO update(LocalDate day, int score, int totalChars, int correctChars,
                                              String avgSpeed, String avgAccuracy, String peakSpeed, String peakAccuracy) {
        DailyStatsUpdateDTO dto = new DailyStatsUpdateDTO();
        dto.setStatDate(day);
        dto.setGameMode("ENDLESS");
        dto.setTotalScore(score);
        dto.setTotalChars(totalChars);
        dto.setCorrectChars(correctChars);
        dto.setAvgSpeed(bd(avgSpeed));
        dto.setAvgAccuracy(bd(avgAccuracy));
        dto.setPeakSpeed(bd(peakSpeed));
        dto.setPeakAccuracy(bd(peakAccuracy));
        return dto;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
