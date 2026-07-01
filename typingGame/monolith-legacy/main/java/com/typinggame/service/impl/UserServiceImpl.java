package com.typinggame.service.impl;


import com.typinggame.dto.user.UserStatsDTO;
import com.typinggame.dto.user.UserUpdateDTO;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.entity.user.UserDailyStats;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.game.MonsterData;
import com.typinggame.entity.leaderboard.LeaderboardEntry;
import com.typinggame.repository.CharacterStatsRepository;
import com.typinggame.repository.GameRecordRepository;
import com.typinggame.repository.MonsterDataRepository;
import com.typinggame.repository.LeaderboardEntryRepository;
import com.typinggame.repository.UserDailyStatsRepository;
import com.typinggame.repository.UserInfoRepository;
import com.typinggame.service.UserService;
import com.typinggame.util.FileStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Slf4j // 核心：加这个注解，自动生成log对象
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private GameRecordRepository gameRecordRepository;

    @Autowired
    private MonsterDataRepository monsterDataRepository;

    @Autowired
    private LeaderboardEntryRepository leaderboardEntryRepository;

    @Autowired
    private CharacterStatsRepository characterStatsRepository;

    @Autowired
    private UserDailyStatsRepository userDailyStatsRepository;

    @Autowired
    private FileStorageUtil fileStorageUtil;

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
            
            // 仅更新非空字段，防止数据被覆盖为null
            if (userUpdateDTO.getNickname() != null) {
                userInfo.setNickname(userUpdateDTO.getNickname());
            }
            if (userUpdateDTO.getAvatar() != null) {
                userInfo.setAvatar(userUpdateDTO.getAvatar());
            }
            if (userUpdateDTO.getSignature() != null) {
                userInfo.setSignature(userUpdateDTO.getSignature());
            }
            // 只有当提供了非空的 username 且不相同时才更新（实际业务中修改用户名通常需要查重）
            if (userUpdateDTO.getUsername() != null && !userUpdateDTO.getUsername().isEmpty()) {
                userInfo.setUsername(userUpdateDTO.getUsername());
            }
            
            return userInfoRepository.save(userInfo);
        }
        return null;
    }

    @Override
    public String uploadAvatar(Integer userId, MultipartFile file) {
        String fileName = fileStorageUtil.storeFile(file);
        String avatarPath = "/uploads/" + fileName;
        Optional<UserInfo> userOptional = userInfoRepository.findById(userId);
        if (userOptional.isPresent()) {
            UserInfo userInfo = userOptional.get();
            userInfo.setAvatar(avatarPath);
            userInfoRepository.save(userInfo);
        }
        return avatarPath;
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
            // For now, other fields like totalWordsTyped are not stored in UserInfo.
            // Can be extended later by aggregating GameRecords if needed.
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
    @Transactional
    public void deleteUserGameData(Integer userId) {
        // 删除与游戏记录相关的明细数据
        monsterDataRepository.deleteByUserId(userId);
        leaderboardEntryRepository.deleteByUserId(userId);
        // 删除每日统计与字符统计
        userDailyStatsRepository.deleteByUserId(userId);
        characterStatsRepository.deleteByUserId(userId);
        // 最后删除游戏记录
        gameRecordRepository.deleteByUserId(userId);
    }

    @Override
    public java.util.List<com.typinggame.entity.user.UserDailyStats> getDailyStats(Integer userId, java.time.LocalDate date, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (date != null) {
            return java.util.Collections.singletonList(userDailyStatsRepository.findByUserIdAndStatDate(userId, date).orElse(null));
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
    public java.lang.Object getDailyStatsSummary(Integer userId, java.lang.String period) {
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
        
        java.util.List<com.typinggame.entity.user.UserDailyStats> stats = userDailyStatsRepository.findByUserIdAndStatDateBetween(userId, startDate, endDate);
        
        java.util.Map<java.lang.String, java.lang.Object> summary = new java.util.HashMap<>();
        int totalGames = 0;
        int totalScore = 0;
        double totalSpeed = 0;
        double totalAccuracy = 0;
        
        for (com.typinggame.entity.user.UserDailyStats stat : stats) {
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
