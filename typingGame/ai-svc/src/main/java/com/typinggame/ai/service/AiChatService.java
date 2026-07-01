package com.typinggame.ai.service;

import com.typinggame.dto.ai.AiChatRequestDTO;
import com.typinggame.dto.ai.AiChatResponseDTO;

public interface AiChatService {
    AiChatResponseDTO chat(Integer userId, AiChatRequestDTO request);
}