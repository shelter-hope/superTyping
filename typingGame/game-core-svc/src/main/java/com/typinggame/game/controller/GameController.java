package com.typinggame.game.controller;

import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 兼容前端旧路径的空操作接口。真实对局逻辑在 {@link GameConfigController}、
 * {@link GameRecordController}、{@link GameRoundController}。
 * 保留这些端点是为了不让旧客户端收到 404；不要在这里扩展业务。
 *
 * @deprecated 新功能请使用 /api/v1/game/config/**、/records/**、/rounds/**
 */
@Deprecated
@RestController
@RequestMapping("/api/v1/game")
public class GameController {

    @PostMapping("/config")
    public ResponseEntity<ResultMessage<Boolean>> saveGameConfig(@RequestBody Object config) {
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @PostMapping("/state")
    public ResponseEntity<ResultMessage<Boolean>> updateGameState(@RequestBody GameStateRequest state) {
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @PostMapping("/params")
    public ResponseEntity<ResultMessage<Boolean>> adjustGameParams(@RequestBody Object params) {
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @PostMapping("/round-data")
    public ResponseEntity<ResultMessage<Boolean>> recordRoundData(@RequestBody Object roundData) {
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @GetMapping("/export/{recordId}")
    public ResponseEntity<ResultMessage<String>> exportGameData(@PathVariable Integer recordId,
                                                                @RequestParam String format) {
        return ResponseEntity.ok(ResultMessage.success("Export successful"));
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
