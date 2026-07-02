package com.typinggame.user.service.impl;

import com.typinggame.common.admin.AdminPageSupport;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.user.repository.FriendRelationRepository;
import com.typinggame.user.repository.UserAppConfigRepository;
import com.typinggame.user.repository.UserDailyStatsRepository;
import com.typinggame.user.repository.UserGameModeConfigRepository;
import com.typinggame.user.repository.UserInfoRepository;
import com.typinggame.user.repository.UserInfoSnapshotRepository;
import com.typinggame.user.repository.UserModelParamsRepository;
import com.typinggame.user.repository.UserMonsterConfigRepository;
import com.typinggame.user.service.InternalAdminService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InternalAdminServiceImpl implements InternalAdminService {

    private final UserInfoRepository userInfoRepository;
    private final FriendRelationRepository friendRelationRepository;
    private final UserAppConfigRepository userAppConfigRepository;
    private final UserGameModeConfigRepository userGameModeConfigRepository;
    private final UserMonsterConfigRepository userMonsterConfigRepository;
    private final UserModelParamsRepository userModelParamsRepository;
    private final UserDailyStatsRepository userDailyStatsRepository;
    private final UserInfoSnapshotRepository userInfoSnapshotRepository;

    public InternalAdminServiceImpl(UserInfoRepository userInfoRepository,
                                    FriendRelationRepository friendRelationRepository,
                                    UserAppConfigRepository userAppConfigRepository,
                                    UserGameModeConfigRepository userGameModeConfigRepository,
                                    UserMonsterConfigRepository userMonsterConfigRepository,
                                    UserModelParamsRepository userModelParamsRepository,
                                    UserDailyStatsRepository userDailyStatsRepository,
                                    UserInfoSnapshotRepository userInfoSnapshotRepository) {
        this.userInfoRepository = userInfoRepository;
        this.friendRelationRepository = friendRelationRepository;
        this.userAppConfigRepository = userAppConfigRepository;
        this.userGameModeConfigRepository = userGameModeConfigRepository;
        this.userMonsterConfigRepository = userMonsterConfigRepository;
        this.userModelParamsRepository = userModelParamsRepository;
        this.userDailyStatsRepository = userDailyStatsRepository;
        this.userInfoSnapshotRepository = userInfoSnapshotRepository;
    }

    @Override
    public Map<String, Object> getUsers(int page, int pageSize, String searchTerm) {
        PageRequest pageable = pageable(page, pageSize);
        if (blank(searchTerm)) {
            return AdminPageSupport.pageMap("users", userInfoRepository.findAll(pageable));
        }
        return AdminPageSupport.pageMap("users", userInfoRepository.findByUsernameContainingOrNicknameContaining(searchTerm, searchTerm, pageable));
    }

    @Override
    @Transactional
    public boolean updateUserRole(Integer userId, String role) {
        UserInfo user = userInfoRepository.findById(userId).orElse(null);
        if (user == null) return false;
        user.setRole(role != null ? role : user.getRole());
        userInfoRepository.save(user);
        return true;
    }

    @Override public Map<String, Object> getFriendRelations(int page, int pageSize) { return AdminPageSupport.pageMap("items", friendRelationRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getUserAppConfigs(int page, int pageSize) { return AdminPageSupport.pageMap("items", userAppConfigRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getUserGameModeConfigs(int page, int pageSize) { return AdminPageSupport.pageMap("items", userGameModeConfigRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getUserMonsterConfigs(int page, int pageSize) { return AdminPageSupport.pageMap("items", userMonsterConfigRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getUserModelParams(int page, int pageSize) { return AdminPageSupport.pageMap("items", userModelParamsRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getUserDailyStats(int page, int pageSize) { return AdminPageSupport.pageMap("items", userDailyStatsRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getUserInfoSnapshots(int page, int pageSize) { return AdminPageSupport.pageMap("items", userInfoSnapshotRepository.findAll(pageable(page, pageSize))); }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private PageRequest pageable(int page, int pageSize) {
        return PageRequest.of(Math.max(0, page - 1), Math.max(1, pageSize));
    }
}
