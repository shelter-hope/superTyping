package com.typinggame.ai.service;

import com.typinggame.dto.ai.AiTrainingSummaryDTO;

public interface AiService {
    AiTrainingSummaryDTO getTrainingSummary(Integer userId);
}