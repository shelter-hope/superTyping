package com.typinggame.game.controller;

import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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