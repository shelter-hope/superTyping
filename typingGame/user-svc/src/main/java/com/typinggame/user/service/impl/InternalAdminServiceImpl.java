package com.typinggame.user.service.impl;

import com.typinggame.common.admin.AdminFilter;
import com.typinggame.common.admin.AdminPageSupport;
import com.typinggame.common.exception.BusinessException;
import com.typinggame.user.entity.UserAppConfig;
import com.typinggame.user.entity.FriendRelation;
import com.typinggame.entity.user.UserDailyStats;
import com.typinggame.user.entity.UserInfo;
import com.typinggame.user.entity.UserInfoSnapshot;
import com.typinggame.user.repository.FriendRelationRepository;
import com.typinggame.user.repository.UserAppConfigRepository;
import com.typinggame.user.repository.UserDailyStatsRepository;
import com.typinggame.user.repository.UserInfoRepository;
import com.typinggame.user.repository.UserInfoSnapshotRepository;
import com.typinggame.user.service.InternalAdminService;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@Service
public class InternalAdminServiceImpl implements InternalAdminService {

    private static final Set<String> ROLES = Set.of("user", "admin");

    private final UserInfoRepository userInfoRepository;
    private final FriendRelationRepository friendRelationRepository;
    private final UserAppConfigRepository userAppConfigRepository;
    private final UserDailyStatsRepository userDailyStatsRepository;
    private final UserInfoSnapshotRepository userInfoSnapshotRepository;

    public InternalAdminServiceImpl(UserInfoRepository userInfoRepository,
                                    FriendRelationRepository friendRelationRepository,
                                    UserAppConfigRepository userAppConfigRepository,
                                    UserDailyStatsRepository userDailyStatsRepository,
                                    UserInfoSnapshotRepository userInfoSnapshotRepository) {
        this.userInfoRepository = userInfoRepository;
        this.friendRelationRepository = friendRelationRepository;
        this.userAppConfigRepository = userAppConfigRepository;
        this.userDailyStatsRepository = userDailyStatsRepository;
        this.userInfoSnapshotRepository = userInfoSnapshotRepository;
    }

    @Override
    public Map<String, Object> getUsers(int page, int pageSize, String searchTerm, String role, Integer status) {
        Specification<UserInfo> spec = AdminFilter.of(UserInfo.class).eq("role", role);
        if (searchTerm != null && !searchTerm.isBlank()) {
            String pattern = "%" + searchTerm.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("nickname")), pattern)));
        }
        if (status != null) {
            // 历史数据 status 为 null，视为正常
            spec = spec.and((root, q, cb) -> {
                Predicate eq = cb.equal(root.get("status"), status);
                return status == UserInfo.STATUS_ACTIVE ? cb.or(eq, cb.isNull(root.get("status"))) : eq;
            });
        }
        return AdminPageSupport.pageMap("users", userInfoRepository.findAll(spec, pageable(page, pageSize, "userId")));
    }

    @Override
    @Transactional
    public boolean updateUserRole(Integer userId, String role) {
        UserInfo user = userInfoRepository.findById(userId).orElse(null);
        if (user == null) return false;
        if (role != null && !ROLES.contains(role)) {
            throw BusinessException.badRequest("角色只能是 user 或 admin");
        }
        user.setRole(role != null ? role : user.getRole());
        userInfoRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public void updateUserStatus(Integer userId, Integer status) {
        if (status == null || (status != UserInfo.STATUS_ACTIVE && status != UserInfo.STATUS_DISABLED)) {
            throw BusinessException.badRequest("状态只能是 0（禁用）或 1（正常）");
        }
        UserInfo user = userInfoRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        if (status == UserInfo.STATUS_DISABLED && "admin".equals(user.getRole())) {
            throw BusinessException.badRequest("不能禁用管理员账号，请先取消其管理员身份");
        }
        user.setStatus(status);
        userInfoRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserProfile(Integer userId, String nickname, String signature) {
        UserInfo user = userInfoRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        if (nickname != null) {
            String trimmed = nickname.trim();
            if (trimmed.isEmpty() || trimmed.length() > 50) {
                throw BusinessException.badRequest("昵称长度必须在1-50个字符之间");
            }
            user.setNickname(trimmed);
        }
        if (signature != null) {
            if (signature.length() > 200) {
                throw BusinessException.badRequest("个性签名不能超过200个字符");
            }
            user.setSignature(signature);
        }
        userInfoRepository.save(user);
    }

    @Override
    public Map<String, Object> getOverview() {
        Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        long newToday = userInfoRepository.count(AdminFilter.of(UserInfo.class).between("createTime", todayStart, null));
        long activeToday = userInfoRepository.count(AdminFilter.of(UserInfo.class).between("lastLoginTime", todayStart, null));
        long disabled = userInfoRepository.count(AdminFilter.of(UserInfo.class).eq("status", UserInfo.STATUS_DISABLED));
        return AdminPageSupport.map(
                "userCount", userInfoRepository.count(),
                "newUsersToday", newToday,
                "activeUsersToday", activeToday,
                "disabledUsers", disabled);
    }

    @Override
    public Map<String, Object> getFriendRelations(int page, int pageSize, Integer userId, Integer friendUserId, Integer relationStatus) {
        AdminFilter<FriendRelation> f = AdminFilter.of(FriendRelation.class)
                .eq("userId", userId).eq("friendUserId", friendUserId).eq("relationStatus", relationStatus);
        return AdminPageSupport.pageMap("items", friendRelationRepository.findAll(f, pageable(page, pageSize, "id")));
    }

    @Override
    public Map<String, Object> getUserAppConfigs(int page, int pageSize, Integer userId) {
        AdminFilter<UserAppConfig> f = AdminFilter.of(UserAppConfig.class).eq("userId", userId);
        return AdminPageSupport.pageMap("items", userAppConfigRepository.findAll(f, pageable(page, pageSize, "configId")));
    }

    @Override
    public Map<String, Object> getUserDailyStats(int page, int pageSize, Integer userId) {
        AdminFilter<UserDailyStats> f = AdminFilter.of(UserDailyStats.class).eq("userId", userId);
        return AdminPageSupport.pageMap("items", userDailyStatsRepository.findAll(f, pageable(page, pageSize, "id")));
    }

    @Override
    public Map<String, Object> getUserInfoSnapshots(int page, int pageSize, Integer userId) {
        AdminFilter<UserInfoSnapshot> f = AdminFilter.of(UserInfoSnapshot.class).eq("userId", userId);
        return AdminPageSupport.pageMap("items", userInfoSnapshotRepository.findAll(f, pageable(page, pageSize, "id")));
    }

    private PageRequest pageable(int page, int pageSize, String idField) {
        return PageRequest.of(Math.max(0, page - 1), Math.min(200, Math.max(1, pageSize)), Sort.by(Sort.Direction.DESC, idField));
    }
}
