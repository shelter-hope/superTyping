package com.typinggame.chat.support.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * user-svc 内部好友接口客户端。
 *
 * <p>好友关系表已迁回 user-svc，chat-svc 不再本地查询 friend_relation；
 * IM 在线状态广播等场景改为通过该客户端获取好友 ID 列表。
 */
@Component
public class UserSvcFriendsClient {
    private static final Logger log = LoggerFactory.getLogger(UserSvcFriendsClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UserSvcFriendsClient(RestTemplate chatRestTemplate,
                                @Value("${chat.friends.user-svc-base-url:http://user-svc/user-svc}") String baseUrl) {
        this.restTemplate = chatRestTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** 返回好友 ID 列表；user-svc 不可用时返回空列表并告警，不阻断 IM 主流程。 */
    public List<Integer> findFriendIds(Integer userId) {
        String url = baseUrl + "/api/v1/internal/friends/ids?userId={userId}";
        try {
            Integer[] friendIds = restTemplate.getForObject(url, Integer[].class, userId);
            return friendIds == null ? List.of() : Arrays.asList(friendIds);
        } catch (Exception exception) {
            log.warn("user-svc 好友 ID 列表查询失败，按无好友处理: {}", exception.getMessage());
            return List.of();
        }
    }
}
