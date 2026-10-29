package com.typinggame.user.service.impl;

import com.typinggame.dto.user.DailyStatsUpdateDTO;
import com.typinggame.dto.user.UserGameSummaryDTO;
import com.typinggame.dto.user.UserProfileDTO;
import com.typinggame.user.entity.UserDailyStats;
import com.typinggame.user.entity.UserInfo;
import com.typinggame.user.repository.UserDailyStatsRepository;
import com.typinggame.user.repository.UserInfoRepository;
import com.typinggame.user.service.InternalUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class InternalUserServiceImpl implements InternalUserService {

    private final UserInfoRepository userInfoRepository;
    private final UserDailyStatsRepository userDailyStatsRepository;

    public InternalUserServiceImpl(UserInfoRepository userInfoRepository,
                                   UserDailyStatsRepository userDailyStatsRepository) {
        this.userInfoRepository = userInfoRepository;
        this.userDailyStatsRepository = userDailyStatsRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(Integer userId) {
        if (userId == null) {
            return null;
        }
        return userInfoRepository.findById(userId).map(InternalUserServiceImpl::toProfile).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileDTO> getProfiles(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<Integer> distinct = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return List.of();
        }
        List<UserProfileDTO> result = new ArrayList<>(distinct.size());
        userInfoRepository.findAllById(distinct).forEach(user -> result.add(toProfile(user)));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getAllUserIds() {
        return userInfoRepository.findAllUserIds();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Integer userId) {
        return userId != null && userInfoRepository.existsById(userId);
    }

    @Override
    @Transactional
    public UserProfileDTO applyGameSummary(Integer userId, UserGameSummaryDTO summary) {
        if (userId == null || summary == null) {
            return null;
        }
        UserInfo user = userInfoRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        int score = summary.getTotalScore() == null ? 0 : summary.getTotalScore();
        if (user.getBestScore() == null || score > user.getBestScore()) {
            user.setBestScore(score);
            user.setBestRound(summary.getRoundNum());
            user.setBestSpeed(summary.getPeakSpeed());
            user.setBestAccuracy(summary.getPeakAccuracy());
        }
        user.setGameNum((user.getGameNum() == null ? 0 : user.getGameNum()) + 1);
        user.setAvgSpeed(summary.getAvgSpeed());
        user.setAvgAccuracy(summary.getAvgAccuracy());
        user.setPeakAccuracy(summary.getPeakAccuracy());
        user.setUpdateTime(new Date());
        return toProfile(userInfoRepository.save(user));
    }

    @Override
    @Transactional
    public void upsertDailyStats(Integer userId, DailyStatsUpdateDTO update) {
        if (userId == null || update == null) {
            return;
        }
        LocalDate statDate = update.getStatDate() == null ? LocalDate.now() : update.getStatDate();
        UserDailyStats stats = userDailyStatsRepository
                .findByUserIdAndStatDate(userId, statDate)
                .orElseGet(() -> {
                    UserDailyStats created = new UserDailyStats();
                    created.setUserId(userId);
                    created.setStatDate(statDate);
                    created.setGameCount(0);
                    created.setTotalScore(0);
                    created.setTotalChars(0);
                    created.setCorrectChars(0);
                    created.setAvgSpeed(BigDecimal.ZERO);
                    created.setAvgAccuracy(BigDecimal.ZERO);
                    created.setPeakSpeed(BigDecimal.ZERO);
                    created.setPeakAccuracy(BigDecimal.ZERO);
                    created.setCreateTime(new Date());
                    return created;
                });
        int oldCount = safeInt(stats.getGameCount());
        int newCount = oldCount + 1;
        stats.setGameCount(newCount);
        stats.setTotalScore(safeInt(stats.getTotalScore()) + safeInt(update.getTotalScore()));
        stats.setTotalChars(safeInt(stats.getTotalChars()) + safeInt(update.getTotalChars()));
        stats.setCorrectChars(safeInt(stats.getCorrectChars()) + safeInt(update.getCorrectChars()));
        stats.setAvgSpeed(weightedAverage(stats.getAvgSpeed(), oldCount, update.getAvgSpeed(), newCount));
        stats.setAvgAccuracy(weightedAverage(stats.getAvgAccuracy(), oldCount, update.getAvgAccuracy(), newCount));
        stats.setPeakSpeed(max(stats.getPeakSpeed(), update.getPeakSpeed()));
        stats.setPeakAccuracy(max(stats.getPeakAccuracy(), update.getPeakAccuracy()));
        if (update.getGameMode() != null) {
            stats.setGameMode(update.getGameMode());
        }
        stats.setUpdateTime(new Date());
        userDailyStatsRepository.save(stats);
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal weightedAverage(BigDecimal oldValue, int oldCount, BigDecimal newValue, int newCount) {
        BigDecimal oldSum = (oldValue == null ? BigDecimal.ZERO : oldValue)
                .multiply(BigDecimal.valueOf(Math.max(0, oldCount)));
        BigDecimal incoming = newValue == null ? BigDecimal.ZERO : newValue;
        return oldSum.add(incoming)
                .divide(BigDecimal.valueOf(Math.max(1, newCount)), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal max(BigDecimal left, BigDecimal right) {
        BigDecimal l = left == null ? BigDecimal.ZERO : left;
        BigDecimal r = right == null ? BigDecimal.ZERO : right;
        return l.max(r);
    }

    private static UserProfileDTO toProfile(UserInfo user) {        return UserProfileDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .signature(user.getSignature())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .enabled(user.getEnabled())
                .bestScore(user.getBestScore())
                .bestRound(user.getBestRound())
                .bestSpeed(user.getBestSpeed())
                .bestAccuracy(user.getBestAccuracy())
                .avgSpeed(user.getAvgSpeed())
                .avgAccuracy(user.getAvgAccuracy())
                .peakAccuracy(user.getPeakAccuracy())
                .gameNum(user.getGameNum())
                .build();
    }
}
