package com.typinggame.user.service;

import java.util.Map;

public interface InternalAdminService {
    Map<String, Object> getUsers(int page, int pageSize, String searchTerm, String role, Integer status);

    boolean updateUserRole(Integer userId, String role);

    /** 禁用 / 启用账号；管理员账号不允许被禁用 */
    void updateUserStatus(Integer userId, Integer status);

    void updateUserProfile(Integer userId, String nickname, String signature);

    Map<String, Object> getOverview();

    Map<String, Object> getFriendRelations(int page, int pageSize, Integer userId, Integer friendUserId, Integer relationStatus);

    Map<String, Object> getUserAppConfigs(int page, int pageSize, Integer userId);

    Map<String, Object> getUserDailyStats(int page, int pageSize, Integer userId);

    Map<String, Object> getUserInfoSnapshots(int page, int pageSize, Integer userId);
}
