package com.typinggame.user.controller;

import com.typinggame.user.service.FriendMessagingGuard;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 服务间好友鉴权。网关屏蔽 /internal/，仅集群内可达。 */
@RestController
@RequestMapping("/api/v1/internal/friends")
public class InternalFriendController {
    private final FriendMessagingGuard friendMessagingGuard;

    public InternalFriendController(FriendMessagingGuard friendMessagingGuard) {
        this.friendMessagingGuard = friendMessagingGuard;
    }

    @GetMapping("/can-message")
    public ResponseEntity<Map<String, Object>> canMessage(@RequestParam Integer senderId,
                                                          @RequestParam Integer receiverId) {
        boolean allowed = friendMessagingGuard.canMessage(senderId, receiverId);
        return ResponseEntity.ok(Map.of("allowed", allowed));
    }
}
