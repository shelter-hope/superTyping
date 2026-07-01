package com.typinggame.controller;

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

/**
 * 用户个性化数学模型参数 API
 */
@RestController
@Tag(name = "用户个性化数学模型参数 API")
@RequestMapping("/api/v1/model/params")
public class UserModelParamsController {

    @Autowired
    private MathModelService mathModelService;

    /**
     * 获取用户个性化模型参数
     * @param userId 用户ID (可选，默认当前用户)
     * @param gameMode 游戏模式
     * @return 模型参数
     */
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

    /**
     * 获取模型参数调整历史
     * @param userId 用户ID (可选，默认当前用户)
     * @param gameMode 游戏模式
     * @param limit 限制数量
     * @return 参数调整历史记录
     */
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
