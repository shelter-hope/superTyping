package com.typinggame.controller;

import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 游戏核心功能 API
 */
@RestController
@Tag(name = "游戏核心功能 API")
@RequestMapping("/api/v1/game")
public class GameController {

    /**
     * 游戏配置保存接口
     * @param config 游戏配置
     * @return 保存结果
     */
    @PostMapping("/config")
    @Operation(summary = "保存游戏配置")
    public ResponseEntity<ResultMessage<Boolean>> saveGameConfig(@RequestBody Object config) {
        // 实现游戏配置保存逻辑
        // 暂时返回成功
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    /**
     * 游戏状态管理接口
     * @param state 游戏状态
     * @return 更新结果
     */
    @PostMapping("/state")
    @Operation(summary = "更新游戏状态")
    public ResponseEntity<ResultMessage<Boolean>> updateGameState(@RequestBody GameStateRequest state) {
        // 实现游戏状态管理逻辑
        // 暂时返回成功
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    /**
     * 游戏参数调整接口
     * @param params 游戏参数
     * @return 调整结果
     */
    @PostMapping("/params")
    @Operation(summary = "调整游戏参数")
    public ResponseEntity<ResultMessage<Boolean>> adjustGameParams(@RequestBody Object params) {
        // 实现游戏参数调整逻辑
        // 暂时返回成功
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    /**
     * 实时轮次数据记录接口
     * @param roundData 轮次数据
     * @return 记录结果
     */
    @PostMapping("/round-data")
    @Operation(summary = "记录实时轮次数据")
    public ResponseEntity<ResultMessage<Boolean>> recordRoundData(@RequestBody Object roundData) {
        // 实现实时轮次数据记录逻辑
        // 暂时返回成功
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    /**
     * 游戏结算数据导出接口
     * @param recordId 记录ID
     * @param format 导出格式
     * @return 导出结果
     */
    @GetMapping("/export/{recordId}")
    @Operation(summary = "导出游戏结算数据")
    public ResponseEntity<ResultMessage<String>> exportGameData(@PathVariable Integer recordId, @RequestParam String format) {
        // 实现游戏结算数据导出逻辑
        // 暂时返回成功
        return ResponseEntity.ok(ResultMessage.success("Export successful"));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }

    // 游戏状态请求类
    private static class GameStateRequest {
        private String state;

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }
}
