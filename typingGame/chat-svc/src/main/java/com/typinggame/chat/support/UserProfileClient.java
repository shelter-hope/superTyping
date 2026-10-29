package com.typinggame.chat.support;

import com.typinggame.dto.user.UserProfileDTO;
import com.typinggame.dto.user.UserProfilesQueryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户数据的集群内访问入口：chat-svc 不再直连 user_info 表。
 *
 * <p>用户档案的唯一来源是 user-svc 的内部接口（网关屏蔽 /internal/，仅集群内可达）。
 * 读取失败按「用户不存在」降级并告警，保证聊天链路不因下游抖动而失败。</p>
 */
@Component
public class UserProfileClient {

    private static final Logger log = LoggerFactory.getLogger(UserProfileClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UserProfileClient(RestTemplate chatRestTemplate,
                             @Value("${chat.user.base-url:http://user-svc/user-svc/api/v1/internal/users}") String baseUrl) {
        this.restTemplate = chatRestTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** 查询单个用户公开档案；用户不存在或下游异常返回 null。 */
    public UserProfileDTO fetchProfile(Integer userId) {
        if (userId == null) {
            return null;
        }
        try {
            return restTemplate.getForObject(baseUrl + "/" + userId, UserProfileDTO.class);
        } catch (HttpClientErrorException.NotFound exception) {
            log.warn("user-svc 用户不存在：userId={}", userId);
            return null;
        } catch (Exception exception) {
            log.warn("user-svc 用户档案查询失败，按用户不存在降级：userId={}, error={}",
                    userId, exception.getMessage());
            return null;
        }
    }

    /** 批量查询用户档案，返回 userId -> 档案；下游异常时返回空 Map。 */
    public Map<Integer, UserProfileDTO> fetchProfiles(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<Integer> userIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        try {
            UserProfileDTO[] profiles = restTemplate.postForObject(
                    baseUrl + "/batch", new UserProfilesQueryDTO(userIds), UserProfileDTO[].class);
            if (profiles == null) {
                return Map.of();
            }
            Map<Integer, UserProfileDTO> result = new LinkedHashMap<>();
            for (UserProfileDTO profile : profiles) {
                if (profile != null && profile.getUserId() != null) {
                    result.put(profile.getUserId(), profile);
                }
            }
            return result;
        } catch (Exception exception) {
            log.warn("user-svc 用户档案批量查询失败，按无数据处理：userIds={}, error={}",
                    userIds, exception.getMessage());
            return Map.of();
        }
    }

    /** 查询全量用户 ID；下游异常时返回空 List。 */
    public List<Integer> allUserIds() {
        try {
            Integer[] userIds = restTemplate.getForObject(baseUrl + "/ids", Integer[].class);
            return userIds == null ? List.of() : Arrays.asList(userIds);
        } catch (Exception exception) {
            log.warn("user-svc 全量用户 ID 查询失败，按空列表降级：error={}", exception.getMessage());
            return List.of();
        }
    }

    /** 判断用户是否存在；下游异常时返回 false。 */
    public boolean exists(Integer userId) {
        if (userId == null) {
            return false;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(
                    baseUrl + "/" + userId + "/exists", Map.class);
            return body != null && Boolean.TRUE.equals(body.get("exists"));
        } catch (Exception exception) {
            log.warn("user-svc 用户存在性查询失败，按不存在降级：userId={}, error={}",
                    userId, exception.getMessage());
            return false;
        }
    }
}
