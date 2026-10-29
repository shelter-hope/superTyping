package com.typinggame.user.controller;

import com.typinggame.dto.user.UserGameSummaryDTO;
import com.typinggame.dto.user.UserProfileDTO;
import com.typinggame.dto.user.UserProfilesQueryDTO;
import com.typinggame.user.service.InternalUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 集群内用户数据接口：用户原型的唯一查询/回写入口。
 * 网关屏蔽 /internal/，仅集群内可达。
 */
@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final InternalUserService internalUserService;

    public InternalUserController(InternalUserService internalUserService) {
        this.internalUserService = internalUserService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDTO> profile(@PathVariable Integer userId) {
        UserProfileDTO profile = internalUserService.getProfile(userId);
        return profile == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(profile);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<UserProfileDTO>> profiles(@RequestBody(required = false) UserProfilesQueryDTO query) {
        return ResponseEntity.ok(internalUserService.getProfiles(query == null ? List.of() : query.getUserIds()));
    }

    @GetMapping("/ids")
    public ResponseEntity<List<Integer>> ids() {
        return ResponseEntity.ok(internalUserService.getAllUserIds());
    }

    @GetMapping("/{userId}/exists")
    public ResponseEntity<Map<String, Object>> exists(@PathVariable Integer userId) {
        return ResponseEntity.ok(Map.of("exists", internalUserService.exists(userId)));
    }

    @PostMapping("/{userId}/game-summary")
    public ResponseEntity<UserProfileDTO> applyGameSummary(@PathVariable Integer userId,
                                                           @RequestBody UserGameSummaryDTO summary) {
        UserProfileDTO profile = internalUserService.applyGameSummary(userId, summary);
        return profile == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(profile);
    }
}
