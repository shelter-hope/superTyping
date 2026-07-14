package com.typinggame.user.controller;

import com.typinggame.entity.config.UserAppConfig;
import com.typinggame.user.service.UserConfigService;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserConfigController {

    private final UserConfigService userConfigService;

    public UserConfigController(UserConfigService userConfigService) {
        this.userConfigService = userConfigService;
    }

    @GetMapping("/config")
    public ResponseEntity<ResultMessage<UserAppConfig>> getConfig() {
        return ResponseEntity.ok(ResultMessage.success(userConfigService.getConfig(getCurrentUserId())));
    }

    @PostMapping("/config")
    public ResponseEntity<ResultMessage<UserAppConfig>> saveConfig(@RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(ResultMessage.success(userConfigService.saveConfig(getCurrentUserId(), updates)));
    }

    @PatchMapping("/sound-settings")
    public ResponseEntity<ResultMessage<Boolean>> updateSoundSettings(@RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(ResultMessage.success(userConfigService.updateSoundSettings(getCurrentUserId(), body.get("enabled"))));
    }

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !StringUtils.hasText(auth.getName())) {
            throw new IllegalStateException("未登录或无法获取用户ID");
        }
        return Integer.parseInt(auth.getName());
    }
}
