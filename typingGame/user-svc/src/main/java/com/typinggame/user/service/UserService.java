package com.typinggame.user.service;

import com.typinggame.dto.user.UserStatsDTO;
import com.typinggame.dto.user.UserUpdateDTO;
import com.typinggame.entity.user.UserInfo;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserInfo getUserById(Integer userId);

    Page<UserInfo> getUsers(Pageable pageable, String searchTerm);

    UserInfo updateUser(Integer userId, UserUpdateDTO userUpdateDTO);

    String uploadAvatar(Integer userId, MultipartFile file);

    UserStatsDTO getUserGameStats(Integer userId);

    boolean updateUserRole(Integer userId, String role);

    void deleteUserGameData(Integer userId);

    java.util.List<com.typinggame.entity.user.UserDailyStats> getDailyStats(Integer userId, java.time.LocalDate date, java.time.LocalDate startDate, java.time.LocalDate endDate);

    java.lang.Object getDailyStatsSummary(Integer userId, java.lang.String period);
}