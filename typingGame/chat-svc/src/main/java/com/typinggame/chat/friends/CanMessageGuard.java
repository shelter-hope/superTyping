package com.typinggame.chat.friends;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 好友鉴权门面。
 * mode=local：仅本地 FriendRelation；
 * mode=user-svc：仅调 user 内部接口；
 * mode=dual：先调 user，失败回退本地（默认，渐进切换）。
 */
@Component
public class CanMessageGuard {
    private final JpaCanMessageAdapter local;
    private final UserSvcCanMessageAdapter remote;
    private final String mode;

    public CanMessageGuard(JpaCanMessageAdapter local,
                           UserSvcCanMessageAdapter remote,
                           @Value("${chat.friends.auth-mode:dual}") String mode) {
        this.local = local;
        this.remote = remote;
        this.mode = mode == null ? "dual" : mode.trim().toLowerCase();
    }

    public void assertCanMessage(Integer senderId, Integer receiverId) {
        if (!canMessage(senderId, receiverId)) {
            throw new IllegalArgumentException("只能给好友发消息");
        }
    }

    public boolean canMessage(Integer senderId, Integer receiverId) {
        return switch (mode) {
            case "user-svc" -> remote.canMessage(senderId, receiverId);
            case "local" -> local.canMessage(senderId, receiverId);
            default -> remote.canMessageOrFallback(senderId, receiverId, local);
        };
    }
}
