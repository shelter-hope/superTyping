package com.typinggame.controller;

import com.typinggame.entity.model.UserModelAdaptLog;
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
 * 玩家模型适配日志 API
 */
@RestController
@Tag(name = "玩家模型适配日志 API")
@RequestMapping("/api/v1/model/adapt/logs")
public class UserModelAdaptLogsController {

    @Autowired
    private MathModelService mathModelService;

    /**
     * 获取模型适配日志
     * @param userId 用户ID (可选，默认当前用户)
     * @param gameRecordId 游戏记录ID (可选)
     * @param limit 限制数量
     * @return 日志列表
     */
    @GetMapping
    @Operation(summary = "获取模型适配日志")
    public ResponseEntity<ResultMessage<List<Object>>> getModelAdaptLogs(
            @RequestParam(name = "userId", required = false) Integer userId,
            @RequestParam(name = "gameRecordId", required = false) Integer gameRecordId,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        if (userId == null) {
            userId = getCurrentUserId();
        }
        List<Object> logs = mathModelService.getModelParamsHistory(userId, "", limit);
        return ResponseEntity.ok(ResultMessage.success(logs));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}
