package com.typinggame.ai.service.impl;

import com.typinggame.ai.repository.AiChatMessageRepository;
import com.typinggame.ai.repository.AiConversationRepository;
import com.typinggame.ai.service.InternalAdminService;
import com.typinggame.common.admin.AdminPageSupport;
import org.springframework.data.domain.PageRequest;
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

    @Override public Map<String, Object> getAiConversations(int page, int pageSize) { return AdminPageSupport.pageMap("items", aiConversationRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getAiChatMessages(int page, int pageSize) { return AdminPageSupport.pageMap("items", aiChatMessageRepository.findAll(pageable(page, pageSize))); }

    private PageRequest pageable(int page, int pageSize) { return PageRequest.of(Math.max(0, page - 1), Math.max(1, pageSize)); }
}
