package com.typinggame.user.service;

import java.util.Map;

public interface InternalAdminService {
    Map<String, Object> getUsers(int page, int pageSize, String searchTerm);

    boolean updateUserRole(Integer userId, String role);

    Map<String, Object> getFriendRelations(int page, int pageSize);

    Map<String, Object> getUserAppConfigs(int page, int pageSize);

    Map<String, Object> getUserGameModeConfigs(int page, int pageSize);

    Map<String, Object> getUserMonsterConfigs(int page, int pageSize);

    Map<String, Object> getUserModelParams(int page, int pageSize);

    Map<String, Object> getUserDailyStats(int page, int pageSize);

    Map<String, Object> getUserInfoSnapshots(int page, int pageSize);
}
