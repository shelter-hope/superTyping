package com.typinggame.user.service.impl;

import com.typinggame.dto.user.UserStatsDTO;
import com.typinggame.dto.user.UserUpdateDTO;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.entity.user.UserDailyStats;
import com.typinggame.user.repository.UserInfoRepository;
import com.typinggame.user.repository.UserDailyStatsRepository;
import com.typinggame.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private UserDailyStatsRepository userDailyStatsRepository;

    @Override
    public UserInfo getUserById(Integer userId) {
        Optional<UserInfo> userOptional = userInfoRepository.findByUserId(userId);
        return userOptional.orElse(null);
    }

    @Override
    public Page<UserInfo> getUsers(Pageable pageable, String searchTerm) {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return userInfoRepository.findByUsernameContainingOrNicknameContaining(searchTerm, searchTerm, pageable);
        }
        return userInfoRepository.findAll(pageable);
    }

    @Override
    public UserInfo updateUser(Integer userId, UserUpdateDTO userUpdateDTO) {
        Optional<UserInfo> userOptional = userInfoRepository.findById(userId);
        if (userOptional.isPresent()) {
            UserInfo userInfo = userOptional.get();

            if (userUpdateDTO.getNickname() != null) {
                userInfo.setNickname(userUpdateDTO.getNickname());
            }
            if (userUpdateDTO.getAvatar() != null) {
                userInfo.setAvatar(userUpdateDTO.getAvatar());
            }
            if (userUpdateDTO.getSignature() != null) {
                userInfo.setSignature(userUpdateDTO.getSignature());
            }
            if (userUpdateDTO.getUsername() != null && !userUpdateDTO.getUsername().isEmpty()) {
                userInfo.setUsername(userUpdateDTO.getUsername());
            }

            return userInfoRepository.save(userInfo);
        }
        return null;
    }

    @Override
    public String uploadAvatar(Integer userId, org.springframework.web.multipart.MultipartFile file) {
        throw new UnsupportedOperationException("文件上传功能暂未迁移，请使用原单体服务");
    }

    @Override
    public UserStatsDTO getUserGameStats(Integer userId) {
        UserInfo user = getUserById(userId);
        UserStatsDTO statsDTO = new UserStatsDTO();
        if (user != null) {
            statsDTO.setUserId(user.getUserId());
            statsDTO.setUsername(user.getUsername());
            statsDTO.setTotalGames(user.getGameNum() != null ? user.getGameNum() : 0);
            statsDTO.setBestScore(user.getBestScore() != null ? user.getBestScore() : 0);
            statsDTO.setAverageAccuracy(user.getAvgAccuracy() != null ? user.getAvgAccuracy().doubleValue() : 0.0);
        }
        return statsDTO;
    }

    @Override
    public boolean updateUserRole(Integer userId, String role) {
        Optional<UserInfo> userOptional = userInfoRepository.findById(userId);
        if (userOptional.isPresent()) {
            UserInfo userInfo = userOptional.get();
            userInfo.setRole(role);
            userInfoRepository.save(userInfo);
            return true;
        }
        return false;
    }

    @Override
    public void deleteUserGameData(Integer userId) {
        userDailyStatsRepository.deleteByUserId(userId);
    }

    @Override
    public List<UserDailyStats> getDailyStats(Integer userId, java.time.LocalDate date, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (date != null) {
            return List.of(userDailyStatsRepository.findByUserIdAndStatDate(userId, date).orElse(null));
        } else if (startDate != null && endDate != null) {
            return userDailyStatsRepository.findByUserIdAndStatDateBetween(userId, startDate, endDate);
        } else if (startDate != null) {
            return userDailyStatsRepository.findByUserIdAndStatDateAfter(userId, startDate);
        } else if (endDate != null) {
            return userDailyStatsRepository.findByUserIdAndStatDateBefore(userId, endDate);
        } else {
            return userDailyStatsRepository.findByUserId(userId);
        }
    }

    @Override
    public Object getDailyStatsSummary(Integer userId, String period) {
        java.time.LocalDate endDate = java.time.LocalDate.now();
        java.time.LocalDate startDate;

        switch (period.toLowerCase()) {
            case "week":
                startDate = endDate.minusWeeks(1);
                break;
            case "month":
                startDate = endDate.minusMonths(1);
                break;
            case "year":
                startDate = endDate.minusYears(1);
                break;
            default:
                startDate = endDate.minusWeeks(1);
        }

        List<UserDailyStats> stats = userDailyStatsRepository.findByUserIdAndStatDateBetween(userId, startDate, endDate);

        Map<String, Object> summary = new java.util.HashMap<>();
        int totalGames = 0;
        int totalScore = 0;
        double totalSpeed = 0;
        double totalAccuracy = 0;

        for (UserDailyStats stat : stats) {
            if (stat != null) {
                totalGames += stat.getGameCount() != null ? stat.getGameCount() : 0;
                totalScore += stat.getTotalScore() != null ? stat.getTotalScore() : 0;
                totalSpeed += stat.getAvgSpeed() != null ? stat.getAvgSpeed().doubleValue() : 0;
                totalAccuracy += stat.getAvgAccuracy() != null ? stat.getAvgAccuracy().doubleValue() : 0;
            }
        }

        summary.put("totalGames", totalGames);
        summary.put("totalScore", totalScore);
        summary.put("averageSpeed", stats.size() > 0 ? totalSpeed / stats.size() : 0);
        summary.put("averageAccuracy", stats.size() > 0 ? totalAccuracy / stats.size() : 0);
        summary.put("period", period);
        summary.put("startDate", startDate);
        summary.put("endDate", endDate);

        return summary;
    }
}