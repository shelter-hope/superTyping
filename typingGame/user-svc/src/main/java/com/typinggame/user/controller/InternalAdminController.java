package com.typinggame.user.controller;

import com.typinggame.user.service.InternalAdminService;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/admin")
public class InternalAdminController {
    private final InternalAdminService internalAdminService;

    public InternalAdminController(InternalAdminService internalAdminService) {
        this.internalAdminService = internalAdminService;
    }

    @GetMapping("/users")
    public ResponseEntity<ResultMessage<Map<String, Object>>> users(@RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "20") int pageSize,
                                                                    @RequestParam(required = false) String searchTerm) {
        return ResponseEntity.ok(ResultMessage.success(internalAdminService.getUsers(page, pageSize, searchTerm)));
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<ResultMessage<Void>> updateRole(@PathVariable Integer userId, @RequestBody Map<String, String> body) {
        return internalAdminService.updateUserRole(userId, body.get("role"))
                ? ResponseEntity.ok(ResultMessage.success())
                : ResponseEntity.badRequest().body(ResultMessage.error("用户不存在"));
    }

    @GetMapping("/friend-relations") public ResponseEntity<ResultMessage<Map<String, Object>>> friendRelations(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getFriendRelations(page, pageSize))); }
    @GetMapping("/user-app-configs") public ResponseEntity<ResultMessage<Map<String, Object>>> userAppConfigs(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getUserAppConfigs(page, pageSize))); }
    @GetMapping("/user-game-mode-configs") public ResponseEntity<ResultMessage<Map<String, Object>>> userGameModeConfigs(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getUserGameModeConfigs(page, pageSize))); }
    @GetMapping("/user-monster-configs") public ResponseEntity<ResultMessage<Map<String, Object>>> userMonsterConfigs(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getUserMonsterConfigs(page, pageSize))); }
    @GetMapping("/user-model-params") public ResponseEntity<ResultMessage<Map<String, Object>>> userModelParams(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getUserModelParams(page, pageSize))); }
    @GetMapping("/user-daily-stats") public ResponseEntity<ResultMessage<Map<String, Object>>> userDailyStats(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getUserDailyStats(page, pageSize))); }
    @GetMapping("/user-info-snapshots") public ResponseEntity<ResultMessage<Map<String, Object>>> userInfoSnapshots(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getUserInfoSnapshots(page, pageSize))); }
}
