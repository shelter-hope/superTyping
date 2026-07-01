package com.typinggame.chat.controller;

import com.typinggame.chat.service.LeaderboardService;
import com.typinggame.dto.leaderboard.LeaderboardDTO;
import com.typinggame.vo.ResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaderboard")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<ResultMessage<List<LeaderboardDTO>>> getLeaderboard(
            @RequestParam(name = "mode", defaultValue = "endless") String mode,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        List<LeaderboardDTO> leaderboard = leaderboardService.getGlobalLeaderboard(mode, limit);
        return ResponseEntity.ok(ResultMessage.success(leaderboard));
    }

    @GetMapping("/my-rank")
    public ResponseEntity<ResultMessage<LeaderboardDTO>> getMyRank(
            @RequestParam(name = "mode", defaultValue = "endless") String mode) {
        Integer userId = getCurrentUserId();
        LeaderboardDTO myRank = leaderboardService.getMyRank(userId, mode);
        return ResponseEntity.ok(ResultMessage.success(myRank));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}