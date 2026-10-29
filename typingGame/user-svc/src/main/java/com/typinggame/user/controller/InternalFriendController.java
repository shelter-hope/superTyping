package com.typinggame.user.controller;

import com.typinggame.user.service.FriendMessagingGuard;
import com.typinggame.user.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 服务间好友鉴权。网关屏蔽 /internal/，仅集群内可达。 */
@RestController
@RequestMapping("/api/v1/internal/friends")
public class InternalFriendController {
    private final FriendMessagingGuard friendMessagingGuard;
    private final FriendService friendService;

    public InternalFriendController(FriendMessagingGuard friendMessagingGuard,
                                    FriendService friendService) {
        this.friendMessagingGuard = friendMessagingGuard;
        this.friendService = friendService;
    }

    @GetMapping("/can-message")
    public ResponseEntity<Map<String, Object>> canMessage(@RequestParam Integer senderId,
                                                          @RequestParam Integer receiverId) {
        boolean allowed = friendMessagingGuard.canMessage(senderId, receiverId);
        return ResponseEntity.ok(Map.of("allowed", allowed));
    }

    /** 好友 ID 列表：chat-svc 广播 IM 在线状态时使用，替代其本地直连 friend_relation。 */
    @GetMapping("/ids")
    public ResponseEntity<List<Integer>> friendIds(@RequestParam Integer userId) {
        return ResponseEntity.ok(friendService.findFriendIds(userId));
    }
}
