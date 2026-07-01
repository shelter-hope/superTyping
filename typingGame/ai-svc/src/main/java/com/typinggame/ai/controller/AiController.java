package com.typinggame.ai.controller;

import com.typinggame.ai.service.AiChatService;
import com.typinggame.ai.service.AiService;
import com.typinggame.dto.ai.AiChatRequestDTO;
import com.typinggame.dto.ai.AiChatResponseDTO;
import com.typinggame.dto.ai.AiTrainingSummaryDTO;
import com.typinggame.vo.ResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    @Autowired
    private AiService aiService;
    @Autowired
    private AiChatService aiChatService;

    @GetMapping("/training-summary")
    public ResponseEntity<ResultMessage<AiTrainingSummaryDTO>> getTrainingSummary() {
        Integer userId = getCurrentUserId();
        return ResponseEntity.ok(ResultMessage.success(aiService.getTrainingSummary(userId)));
    }

    @PostMapping("/chat")
    public ResponseEntity<ResultMessage<AiChatResponseDTO>> chat(@RequestBody AiChatRequestDTO request) {
        Integer userId = getCurrentUserId();
        return ResponseEntity.ok(ResultMessage.success(aiChatService.chat(userId, request)));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}