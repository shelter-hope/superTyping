package com.typinggame.user.service.impl;

import com.typinggame.dto.user.UserStatsDTO;
import com.typinggame.dto.user.UserUpdateDTO;
import com.typinggame.user.entity.UserInfo;
import com.typinggame.entity.user.UserDailyStats;
import com.typinggame.user.repository.UserInfoRepository;
import com.typinggame.user.repository.UserDailyStatsRepository;
import com.typinggame.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final Path AVATAR_DIR = Paths.get("uploads", "avatars");
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

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
    public String uploadAvatar(Integer userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("头像文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("头像文件不能超过2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("仅支持 jpg、png、webp、gif 头像文件");
        }

        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        String fileName = UUID.randomUUID() + extensionOf(contentType);
        Path target = AVATAR_DIR.resolve(fileName).normalize();
        try {
            Files.createDirectories(AVATAR_DIR);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("保存头像文件失败, userId={}", userId, e);
            throw new IllegalStateException("头像文件保存失败");
        }

        String avatarUrl = "/uploads/avatars/" + fileName;
        userInfo.setAvatar(avatarUrl);
        userInfo.setUpdateTime(new Date());
        userInfoRepository.save(userInfo);
        return avatarUrl;
    }

    private String extensionOf(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
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
