package com.typinggame.chat.controller;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.typinggame.chat.service.InternalAdminService;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "internal", description = "服务间内部接口（网关对 /internal/ 返回 404）")
@RestController
@RequestMapping("/api/v1/internal/admin")
public class InternalAdminController {
    private final InternalAdminService internalAdminService;

    public InternalAdminController(InternalAdminService internalAdminService) {
        this.internalAdminService = internalAdminService;
    }

    @GetMapping("/user-conversations")
    public ResponseEntity<ResultMessage<Map<String, Object>>> conversations(@RequestParam(defaultValue = "1") int page,
                                                                            @RequestParam(defaultValue = "20") int pageSize,
                                                                            @RequestParam(required = false) Integer userId) {
        return ok(internalAdminService.getUserConversations(page, pageSize, userId));
    }

    @GetMapping("/friend-chat-messages")
    public ResponseEntity<ResultMessage<Map<String, Object>>> messages(@RequestParam Integer conversationId,
                                                                       @RequestParam(defaultValue = "1") int page,
                                                                       @RequestParam(defaultValue = "20") int pageSize) {
        return ok(internalAdminService.getFriendChatMessages(page, pageSize, conversationId));
    }

    @GetMapping("/system-notifications")
    public ResponseEntity<ResultMessage<Map<String, Object>>> notifications(@RequestParam(defaultValue = "1") int page,
                                                                            @RequestParam(defaultValue = "20") int pageSize,
                                                                            @RequestParam(required = false) Integer userId,
                                                                            @RequestParam(required = false) Integer notificationType,
                                                                            @RequestParam(required = false) Boolean isRead) {
        return ok(internalAdminService.getSystemNotifications(page, pageSize, userId, notificationType, isRead));
    }

    @PostMapping("/system-notifications")
    public ResponseEntity<ResultMessage<Map<String, Object>>> publish(@RequestBody Map<String, Object> body) {
        Integer userId = body.get("userId") == null || body.get("userId").toString().isBlank()
                ? null : Integer.valueOf(body.get("userId").toString());
        Integer type = body.get("notificationType") == null ? null : Integer.valueOf(body.get("notificationType").toString());
        int count = internalAdminService.publishNotification(userId,
                (String) body.get("title"), (String) body.get("content"), type);
        return ok(Map.of("count", count));
    }

    @DeleteMapping("/system-notifications/{id}")
    public ResponseEntity<ResultMessage<Void>> deleteNotification(@PathVariable Integer id) {
        internalAdminService.deleteNotification(id);
        return ResponseEntity.ok(ResultMessage.success());
    }

    @GetMapping("/overview/chat")
    public ResponseEntity<ResultMessage<Map<String, Object>>> overview() {
        return ok(internalAdminService.getOverview());
    }

    private ResponseEntity<ResultMessage<Map<String, Object>>> ok(Map<String, Object> data) {
        return ResponseEntity.ok(ResultMessage.success(data));
    }
}
