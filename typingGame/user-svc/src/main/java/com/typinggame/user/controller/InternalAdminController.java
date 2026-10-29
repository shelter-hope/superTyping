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
                                                                    @RequestParam(required = false) String searchTerm,
                                                                    @RequestParam(required = false) String role,
                                                                    @RequestParam(required = false) Integer status) {
        return ok(internalAdminService.getUsers(page, pageSize, searchTerm, role, status));
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<ResultMessage<Void>> updateRole(@PathVariable Integer userId, @RequestBody Map<String, String> body) {
        return internalAdminService.updateUserRole(userId, body.get("role"))
                ? ResponseEntity.ok(ResultMessage.success())
                : ResponseEntity.badRequest().body(ResultMessage.error("用户不存在"));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<ResultMessage<Void>> updateStatus(@PathVariable Integer userId, @RequestBody Map<String, Integer> body) {
        internalAdminService.updateUserStatus(userId, body.get("status"));
        return ResponseEntity.ok(ResultMessage.success());
    }

    @PatchMapping("/users/{userId}/profile")
    public ResponseEntity<ResultMessage<Void>> updateProfile(@PathVariable Integer userId, @RequestBody Map<String, String> body) {
        internalAdminService.updateUserProfile(userId, body.get("nickname"), body.get("signature"));
        return ResponseEntity.ok(ResultMessage.success());
    }

    @GetMapping("/overview/users")
    public ResponseEntity<ResultMessage<Map<String, Object>>> overview() {
        return ok(internalAdminService.getOverview());
    }

    @GetMapping("/friend-relations")
    public ResponseEntity<ResultMessage<Map<String, Object>>> friendRelations(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize,
                                                                              @RequestParam(required = false) Integer userId, @RequestParam(required = false) Integer friendUserId,
                                                                              @RequestParam(required = false) Integer relationStatus) {
        return ok(internalAdminService.getFriendRelations(page, pageSize, userId, friendUserId, relationStatus));
    }

    @GetMapping("/user-app-configs")
    public ResponseEntity<ResultMessage<Map<String, Object>>> userAppConfigs(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize,
                                                                             @RequestParam(required = false) Integer userId) {
        return ok(internalAdminService.getUserAppConfigs(page, pageSize, userId));
    }

    @GetMapping("/user-daily-stats")
    public ResponseEntity<ResultMessage<Map<String, Object>>> userDailyStats(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize,
                                                                             @RequestParam(required = false) Integer userId) {
        return ok(internalAdminService.getUserDailyStats(page, pageSize, userId));
    }

    @GetMapping("/user-info-snapshots")
    public ResponseEntity<ResultMessage<Map<String, Object>>> userInfoSnapshots(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize,
                                                                                @RequestParam(required = false) Integer userId) {
        return ok(internalAdminService.getUserInfoSnapshots(page, pageSize, userId));
    }

    private ResponseEntity<ResultMessage<Map<String, Object>>> ok(Map<String, Object> data) {
        return ResponseEntity.ok(ResultMessage.success(data));
    }
}
