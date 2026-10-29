package com.typinggame.user.service.impl;

import com.typinggame.dto.user.UserGameSummaryDTO;
import com.typinggame.dto.user.UserProfileDTO;
import com.typinggame.user.entity.UserInfo;
import com.typinggame.user.repository.UserInfoRepository;
import com.typinggame.user.service.InternalUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class InternalUserServiceImpl implements InternalUserService {

    private final UserInfoRepository userInfoRepository;

    public InternalUserServiceImpl(UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
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

    private static UserProfileDTO toProfile(UserInfo user) {
        return UserProfileDTO.builder()
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
