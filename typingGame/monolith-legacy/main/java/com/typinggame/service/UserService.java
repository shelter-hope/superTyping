package com.typinggame.service;

import com.typinggame.dto.user.UserStatsDTO;
import com.typinggame.dto.user.UserUpdateDTO;
import com.typinggame.entity.user.UserInfo;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 处理用户相关业务逻辑
 */
public interface UserService {
    /**
     * 根据ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    UserInfo getUserById(Integer userId);

    /**
     * 获取所有用户（分页）
     * @param pageable 分页信息
     * @param searchTerm 搜索关键词
     * @return 用户分页
     */
    Page<UserInfo> getUsers(Pageable pageable, String searchTerm);

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param userUpdateDTO 用户更新请求
     * @return 更新后的用户信息
     */
    UserInfo updateUser(Integer userId, UserUpdateDTO userUpdateDTO);

    /**
     * 上传用户头像
     * @param userId 用户ID
     * @param file 头像文件
     * @return 头像URL
     */
    String uploadAvatar(Integer userId, MultipartFile file);

    /**
     * 获取用户游戏统计信息
     * @param userId 用户ID
     * @return 用户统计信息
     */
    UserStatsDTO getUserGameStats(Integer userId);

    /**
     * 更新用户角色（管理员）
     * @param userId 用户ID
     * @param role 角色名称
     * @return 更新结果
     */
    boolean updateUserRole(Integer userId, String role);

    /**
     * 删除用户游戏数据
     * @param userId 用户ID
     */
    void deleteUserGameData(Integer userId);

    /**
     * 获取用户每日统计数据
     * @param userId 用户ID
     * @param date 日期
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日统计数据列表
     */
    java.util.List<com.typinggame.entity.user.UserDailyStats> getDailyStats(Integer userId, java.time.LocalDate date, java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 获取用户每日统计摘要
     * @param userId 用户ID
     * @param period 周期
     * @return 统计摘要数据
     */
    java.lang.Object getDailyStatsSummary(Integer userId, java.lang.String period);
}
