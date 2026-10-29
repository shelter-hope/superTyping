package com.typinggame.chat.friends;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 好友鉴权门面。
 *
 * <p>好友关系表已迁回 user-svc，chat-svc 不再直连 friend_relation，只保留 user-svc 内部接口一种实现。
 * {@code chat.friends.auth-mode} 配置键保留兼容，但 local/dual 回退已移除；
 * user-svc 调用失败时按“非好友”拒绝并告警，绝不静默放行。
 */
@Component
public class CanMessageGuard {
    private static final Logger log = LoggerFactory.getLogger(CanMessageGuard.class);

    private final UserSvcCanMessageAdapter remote;

    public CanMessageGuard(UserSvcCanMessageAdapter remote,
                           @Value("${chat.friends.auth-mode:user-svc}") String authMode) {
        this.remote = remote;
        String mode = authMode == null ? "user-svc" : authMode.trim().toLowerCase();
        if (!"user-svc".equals(mode)) {
            log.info("chat.friends.auth-mode={} 已废弃，好友鉴权统一走 user-svc 内部接口", mode);
        }
    }

    public void assertCanMessage(Integer senderId, Integer receiverId) {
        if (!canMessage(senderId, receiverId)) {
            throw new IllegalArgumentException("只能给好友发消息");
        }
    }

    public boolean canMessage(Integer senderId, Integer receiverId) {
        try {
            return remote.canMessage(senderId, receiverId);
        } catch (Exception exception) {
            log.warn("user-svc 好友鉴权失败，按非好友拒绝: {}", exception.getMessage());
            return false;
        }
    }
}
