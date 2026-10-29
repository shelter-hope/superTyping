package com.typinggame.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.ai.repository.AiChatMessageRepository;
import com.typinggame.ai.repository.AiConversationRepository;
import com.typinggame.ai.service.AiService;
import com.typinggame.ai.support.UserProfileClient;
import com.typinggame.dto.ai.AiChatRequestDTO;
import com.typinggame.dto.ai.AiChatResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private AiConversationRepository conversationRepository;

    @Mock
    private AiChatMessageRepository messageRepository;

    @Mock
    private UserProfileClient userProfileClient;

    @Mock
    private AiService aiService;

    @Mock(name = "aiRestTemplate")
    private RestTemplate aiRestTemplate;

    @Mock
    private AiChatServiceImpl.AiLlmConfig.AiLlmProperties aiLlmProperties;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiChatServiceImpl aiChatService;

    @Test
    void rejectsBlankMessageWithoutTouchingStorage() {
        AiChatRequestDTO request = new AiChatRequestDTO();
        request.setMessage("   ");

        AiChatResponseDTO response = aiChatService.chat(7, request);

        assertThat(response.getReply()).isEqualTo("请输入有效内容。");
        assertThat(response.getConversationId()).isZero();
        verifyNoInteractions(conversationRepository, messageRepository, userProfileClient);
    }

    @Test
    void rejectsMissingRequest() {
        AiChatResponseDTO response = aiChatService.chat(7, null);

        assertThat(response.getReply()).isEqualTo("请输入有效内容。");
        assertThat(response.getConversationId()).isZero();
        verifyNoInteractions(conversationRepository, messageRepository);
    }
}
