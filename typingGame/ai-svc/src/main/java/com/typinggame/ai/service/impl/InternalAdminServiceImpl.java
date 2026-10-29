package com.typinggame.ai.service.impl;

import com.typinggame.ai.repository.AiChatMessageRepository;
import com.typinggame.ai.repository.AiConversationRepository;
import com.typinggame.ai.service.InternalAdminService;
import com.typinggame.common.admin.AdminFilter;
import com.typinggame.common.admin.AdminPageSupport;
import com.typinggame.ai.entity.AiChatMessage;
import com.typinggame.ai.entity.AiConversation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InternalAdminServiceImpl implements InternalAdminService {
    private final AiConversationRepository aiConversationRepository;
    private final AiChatMessageRepository aiChatMessageRepository;

    public InternalAdminServiceImpl(AiConversationRepository aiConversationRepository,
                                    AiChatMessageRepository aiChatMessageRepository) {
        this.aiConversationRepository = aiConversationRepository;
        this.aiChatMessageRepository = aiChatMessageRepository;
    }

    @Override
    public Map<String, Object> getAiConversations(int page, int pageSize, Integer userId, Integer aiRobotId) {
        AdminFilter<AiConversation> f = AdminFilter.of(AiConversation.class).eq("userId", userId).eq("aiRobotId", aiRobotId);
        return AdminPageSupport.pageMap("items", aiConversationRepository.findAll(f, pageable(page, pageSize)));
    }

    @Override
    public Map<String, Object> getAiChatMessages(int page, int pageSize, Integer conversationId) {
        AdminFilter<AiChatMessage> f = AdminFilter.of(AiChatMessage.class).eq("conversation.id", conversationId);
        return AdminPageSupport.pageMap("items", aiChatMessageRepository.findAll(f, pageable(page, pageSize)));
    }

    private PageRequest pageable(int page, int pageSize) {
        return PageRequest.of(Math.max(0, page - 1), Math.min(200, Math.max(1, pageSize)), Sort.by(Sort.Direction.DESC, "id"));
    }
}
