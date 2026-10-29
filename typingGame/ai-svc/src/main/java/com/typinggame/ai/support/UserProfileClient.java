package com.typinggame.ai.support;

import com.typinggame.dto.user.UserProfileDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * 用户数据的集群内访问入口：ai-svc 不再直连 user_info 表。
 *
 * <p>用户档案的唯一来源是 user-svc 的内部接口（网关屏蔽 /internal/，仅集群内可达）。
 * 读取失败按「用户不存在」降级并告警，保证聊天链路不因下游抖动而失败。</p>
 */
@Component
public class UserProfileClient {

    private static final Logger log = LoggerFactory.getLogger(UserProfileClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UserProfileClient(@Qualifier("restTemplate") RestTemplate restTemplate,
                             @Value("${ai.user.base-url:http://user-svc/user-svc/api/v1/internal/users}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /** 查询用户公开档案；用户不存在或下游异常返回 null。 */
    public UserProfileDTO fetchProfile(Integer userId) {
        if (userId == null) {
            return null;
        }
        try {
            return restTemplate.getForObject(baseUrl + "/" + userId, UserProfileDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("用户不存在：userId={}", userId);
            return null;
        } catch (Exception e) {
            log.warn("获取用户档案失败，按用户不存在降级：userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    public boolean exists(Integer userId) {
        return fetchProfile(userId) != null;
    }
}
