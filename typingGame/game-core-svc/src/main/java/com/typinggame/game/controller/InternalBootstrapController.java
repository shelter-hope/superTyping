package com.typinggame.game.controller;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.typinggame.game.service.UserBootstrapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 注册后对局配置 bootstrap。网关屏蔽 /internal/。 */
@Tag(name = "internal", description = "服务间内部接口（网关对 /internal/ 返回 404）")
@RestController
@RequestMapping("/api/v1/internal")
public class InternalBootstrapController {
    private final UserBootstrapService userBootstrapService;

    public InternalBootstrapController(UserBootstrapService userBootstrapService) {
        this.userBootstrapService = userBootstrapService;
    }

    @PostMapping("/bootstrap-user/{userId}")
    public ResponseEntity<Map<String, Object>> bootstrap(@PathVariable Integer userId) {
        userBootstrapService.bootstrapUser(userId);
        return ResponseEntity.ok(Map.of("ok", true, "userId", userId));
    }
}
