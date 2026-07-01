package com.typinggame.controller.ai;

import com.typinggame.dto.ai.AiChatRequestDTO;
import com.typinggame.dto.ai.AiChatResponseDTO;
import com.typinggame.dto.ai.AiTrainingSummaryDTO;
import com.typinggame.service.AiChatService;
import com.typinggame.service.AiService;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "AI 助手 API")
@RequestMapping("/api/v1/ai")
public class AiController {

    @Autowired
    private AiService aiService;
    @Autowired
    private AiChatService aiChatService;

    @GetMapping("/training-summary")
    @Operation(summary = "获取训练数据摘要（供 AI 提示词使用）")
    public ResponseEntity<ResultMessage<AiTrainingSummaryDTO>> getTrainingSummary() {
        Integer userId = getCurrentUserId();
        return ResponseEntity.ok(ResultMessage.success(aiService.getTrainingSummary(userId)));
    }

    @PostMapping("/chat")
    @Operation(summary = "AI 对话（统一接口，存储会话并调用 LLM）")
    public ResponseEntity<ResultMessage<AiChatResponseDTO>> chat(@RequestBody AiChatRequestDTO request) {
        Integer userId = getCurrentUserId();
        return ResponseEntity.ok(ResultMessage.success(aiChatService.chat(userId, request)));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}