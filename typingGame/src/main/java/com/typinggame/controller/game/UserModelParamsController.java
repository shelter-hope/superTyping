package com.typinggame.controller.game;

import com.typinggame.entity.model.UserModelParams;
import com.typinggame.service.MathModelService;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "用户个性化数学模型参数 API")
@RequestMapping("/api/v1/model/params")
public class UserModelParamsController {

    @Autowired
    private MathModelService mathModelService;

    @GetMapping
    @Operation(summary = "获取用户个性化模型参数")
    public ResponseEntity<ResultMessage<UserModelParams>> getModelParams(
            @RequestParam(name = "userId", required = false) Integer userId,
            @RequestParam(name = "gameMode", defaultValue = "ENDLESS_ADAPT") String gameMode) {
        if (userId == null) {
            userId = getCurrentUserId();
        }
        UserModelParams params = mathModelService.getUserModelParams(userId, gameMode);
        return ResponseEntity.ok(ResultMessage.success(params));
    }

    @GetMapping("/history")
    @Operation(summary = "获取模型参数调整历史")
    public ResponseEntity<ResultMessage<List<Object>>> getModelParamsHistory(
            @RequestParam(name = "userId", required = false) Integer userId,
            @RequestParam(name = "gameMode", defaultValue = "ENDLESS_ADAPT") String gameMode,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        if (userId == null) {
            userId = getCurrentUserId();
        }
        List<Object> history = mathModelService.getModelParamsHistory(userId, gameMode, limit);
        return ResponseEntity.ok(ResultMessage.success(history));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}