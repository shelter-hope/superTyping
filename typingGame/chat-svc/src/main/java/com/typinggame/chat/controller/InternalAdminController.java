package com.typinggame.chat.controller;

import com.typinggame.chat.service.InternalAdminService;
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

    @GetMapping("/system-dictionaries") public ResponseEntity<ResultMessage<Map<String, Object>>> systemDictionaries(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) String name, @RequestParam(required = false) String category) { return ok(internalAdminService.getSystemDictionaries(page, pageSize, name, category)); }
    @GetMapping("/custom-dictionaries") public ResponseEntity<ResultMessage<Map<String, Object>>> customDictionaries(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getCustomDictionaries(page, pageSize)); }
    @GetMapping("/leaderboard-entries") public ResponseEntity<ResultMessage<Map<String, Object>>> leaderboardEntries(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getLeaderboardEntries(page, pageSize)); }
    @GetMapping("/user-conversations") public ResponseEntity<ResultMessage<Map<String, Object>>> conversations(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getUserConversations(page, pageSize)); }
    @GetMapping("/friend-chat-messages") public ResponseEntity<ResultMessage<Map<String, Object>>> messages(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getFriendChatMessages(page, pageSize)); }
    @GetMapping("/system-notifications") public ResponseEntity<ResultMessage<Map<String, Object>>> notifications(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getSystemNotifications(page, pageSize)); }
    @GetMapping("/system-dictionary-words") public ResponseEntity<ResultMessage<Map<String, Object>>> systemWords(@RequestParam Integer dictId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int pageSize) { return ok(internalAdminService.getSystemDictionaryWords(dictId, page, pageSize)); }
    @GetMapping("/custom-dictionary-words") public ResponseEntity<ResultMessage<Map<String, Object>>> customWords(@RequestParam Integer dictId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int pageSize) { return ok(internalAdminService.getCustomDictionaryWords(dictId, page, pageSize)); }

    private ResponseEntity<ResultMessage<Map<String, Object>>> ok(Map<String, Object> data) { return ResponseEntity.ok(ResultMessage.success(data)); }
}
