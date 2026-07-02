package com.typinggame.ai.controller;

import com.typinggame.ai.service.InternalAdminService;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/admin")
public class InternalAdminController {
    private final InternalAdminService internalAdminService;

    public InternalAdminController(InternalAdminService internalAdminService) {
        this.internalAdminService = internalAdminService;
    }

    @GetMapping("/ai-conversations") public ResponseEntity<ResultMessage<Map<String, Object>>> conversations(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getAiConversations(page, pageSize))); }
    @GetMapping("/ai-chat-messages") public ResponseEntity<ResultMessage<Map<String, Object>>> messages(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getAiChatMessages(page, pageSize))); }
}
