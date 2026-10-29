package com.typinggame.chat.friends;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** user-svc 内部好友鉴权接口客户端；失败由 {@link CanMessageGuard} 按拒绝处理。 */
@Component
public class UserSvcCanMessageAdapter implements CanMessagePort {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UserSvcCanMessageAdapter(RestTemplate chatRestTemplate,
                                    @Value("${chat.friends.user-svc-base-url:http://user-svc/user-svc}") String baseUrl) {
        this.restTemplate = chatRestTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public boolean canMessage(Integer senderId, Integer receiverId) {
        String url = baseUrl + "/api/v1/internal/friends/can-message?senderId={senderId}&receiverId={receiverId}";
        @SuppressWarnings("unchecked")
        Map<String, Object> body = restTemplate.getForObject(url, Map.class, senderId, receiverId);
        if (body == null || body.get("allowed") == null) {
            throw new IllegalStateException("user-svc 好友鉴权无响应");
        }
        return Boolean.TRUE.equals(body.get("allowed"));
    }
}
