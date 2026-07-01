package com.typinggame.controller.game;

import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "游戏核心功能 API")
@RequestMapping("/api/v1/game")
public class GameController {

    @PostMapping("/config")
    @Operation(summary = "保存游戏配置")
    public ResponseEntity<ResultMessage<Boolean>> saveGameConfig(@RequestBody Object config) {
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @PostMapping("/state")
    @Operation(summary = "更新游戏状态")
    public ResponseEntity<ResultMessage<Boolean>> updateGameState(@RequestBody GameStateRequest state) {
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @PostMapping("/params")
    @Operation(summary = "调整游戏参数")
    public ResponseEntity<ResultMessage<Boolean>> adjustGameParams(@RequestBody Object params) {
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @PostMapping("/round-data")
    @Operation(summary = "记录实时轮次数据")
    public ResponseEntity<ResultMessage<Boolean>> recordRoundData(@RequestBody Object roundData) {
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @GetMapping("/export/{recordId}")
    @Operation(summary = "导出游戏结算数据")
    public ResponseEntity<ResultMessage<String>> exportGameData(@PathVariable Integer recordId, @RequestParam String format) {
        return ResponseEntity.ok(ResultMessage.success("Export successful"));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }

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