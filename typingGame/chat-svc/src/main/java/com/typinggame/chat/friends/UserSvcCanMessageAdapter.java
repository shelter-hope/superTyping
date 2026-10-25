package com.typinggame.chat.friends;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class UserSvcCanMessageAdapter {
    private static final Logger log = LoggerFactory.getLogger(UserSvcCanMessageAdapter.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UserSvcCanMessageAdapter(RestTemplate chatRestTemplate,
                                    @Value("${chat.friends.user-svc-base-url:http://user-svc/user-svc}") String baseUrl) {
        this.restTemplate = chatRestTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public boolean canMessage(Integer senderId, Integer receiverId) {
        String url = baseUrl + "/api/v1/internal/friends/can-message?senderId={senderId}&receiverId={receiverId}";
        @SuppressWarnings("unchecked")
        Map<String, Object> body = restTemplate.getForObject(url, Map.class, senderId, receiverId);
        if (body == null || body.get("allowed") == null) {
            throw new IllegalStateException("user-svc 好友鉴权无响应");
        }
        return Boolean.TRUE.equals(body.get("allowed"));
    }

    public boolean canMessageOrFallback(Integer senderId, Integer receiverId, CanMessagePort fallback) {
        try {
            return canMessage(senderId, receiverId);
        } catch (Exception exception) {
            log.warn("user-svc 好友鉴权失败，回退本地查询: {}", exception.getMessage());
            return fallback.canMessage(senderId, receiverId);
        }
    }
}
