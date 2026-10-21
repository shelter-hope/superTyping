package com.typinggame.ai.service;

import java.util.Map;

public interface InternalAdminService {
    Map<String, Object> getAiConversations(int page, int pageSize, Integer userId, Integer aiRobotId);
    Map<String, Object> getAiChatMessages(int page, int pageSize, Integer conversationId);
}
