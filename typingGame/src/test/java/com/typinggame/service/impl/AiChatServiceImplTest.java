package com.typinggame.service.impl;

import com.typinggame.config.AiLlmConfig;
import com.typinggame.dto.ai.AiChatRequestDTO;
import com.typinggame.dto.ai.AiChatResponseDTO;
import com.typinggame.dto.ai.AiTrainingSummaryDTO;
import com.typinggame.entity.message.AiChatMessage;
import com.typinggame.entity.message.AiConversation;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.repository.AiChatMessageRepository;
import com.typinggame.repository.AiConversationRepository;
import com.typinggame.repository.UserInfoRepository;
import com.typinggame.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AI 对话服务单元测试：空消息、无用户、未配置 LLM 时的行为
 */
@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private AiConversationRepository conversationRepository;
    @Mock
    private AiChatMessageRepository messageRepository;
    @Mock
    private UserInfoRepository userInfoRepository;
    @Mock
    private AiService aiService;
    @Mock
    private RestTemplate aiRestTemplate;
    @Mock
    private AiLlmConfig.AiLlmProperties aiLlmProperties;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private AiChatServiceImpl aiChatService;

    private UserInfo user;
    private AiConversation conv;
    private AiLlmConfig.AiLlmProperties.Llm llm;

    @BeforeEach
    void setUp() {
        user = new UserInfo();
        user.setUserId(1);
        user.setUsername("test");
        conv = new AiConversation();
        conv.setId(10);
        conv.setUser(user);
        conv.setAiRobotId(1);
        conv.setConversationTitle("默认");
        llm = org.mockito.Mockito.mock(AiLlmConfig.AiLlmProperties.Llm.class);
        lenient().when(aiLlmProperties.getLlm()).thenReturn(llm);
        lenient().when(aiLlmProperties.getDefaultRobotId()).thenReturn(1);
        lenient().when(aiLlmProperties.getDefaultConversationTitle()).thenReturn("默认");
    }

    @Test
    void chat_nullRequest_returnsPromptToEnterValidContent() {
        AiChatResponseDTO res = aiChatService.chat(1, null);
        assertThat(res.getReply()).contains("请输入有效内容");
        assertThat(res.getConversationId()).isEqualTo(0);
    }

    @Test
    void chat_blankMessage_returnsPromptToEnterValidContent() {
        AiChatRequestDTO req = new AiChatRequestDTO();
        req.setMessage("   ");
        AiChatResponseDTO res = aiChatService.chat(1, req);
        assertThat(res.getReply()).contains("请输入有效内容");
        assertThat(res.getConversationId()).isEqualTo(0);
    }

    @Test
    void chat_userNotFound_returnsUserNotExist() {
        when(userInfoRepository.findByUserId(1)).thenReturn(Optional.empty());
        AiChatRequestDTO req = new AiChatRequestDTO();
        req.setMessage("hello");
        AiChatResponseDTO res = aiChatService.chat(1, req);
        assertThat(res.getReply()).contains("用户不存在");
        assertThat(res.getConversationId()).isEqualTo(0);
    }

    @Test
    void chat_llmNotConfigured_savesConversationAndMessages_returnsPlaceholderReply() {
        when(userInfoRepository.findByUserId(1)).thenReturn(Optional.of(user));
        when(llm.getUrl()).thenReturn(null);
        when(conversationRepository.findFirstByUser_UserIdAndAiRobotIdAndConversationTitleOrderByUpdateTimeDesc(1, 1, "默认"))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(inv -> {
            AiConversation c = inv.getArgument(0);
            if (c.getId() == null) c.setId(10);
            return c;
        });
        when(messageRepository.save(any(AiChatMessage.class))).thenAnswer(inv -> {
            AiChatMessage m = inv.getArgument(0);
            m.setId(m.getMessageRole() == 1 ? 101 : 102);
            return m;
        });
        when(aiService.getTrainingSummary(1)).thenReturn(
                AiTrainingSummaryDTO.builder().contextForPrompt("用户摘要").build());

        AiChatRequestDTO req = new AiChatRequestDTO();
        req.setMessage("你好");
        AiChatResponseDTO res = aiChatService.chat(1, req);

        assertThat(res.getReply()).contains("LLM 未配置");
        assertThat(res.getConversationId()).isEqualTo(10);
        assertThat(res.getMessageId()).isEqualTo(102);
        verify(conversationRepository, times(2)).save(any(AiConversation.class));
        verify(messageRepository, times(2)).save(any(AiChatMessage.class));
        ArgumentCaptor<AiChatMessage> captor = ArgumentCaptor.forClass(AiChatMessage.class);
        verify(messageRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getMessageContent()).isEqualTo("你好");
        assertThat(captor.getAllValues().get(0).getMessageRole()).isEqualTo(1);
        assertThat(captor.getAllValues().get(1).getMessageContent()).contains("LLM 未配置");
        assertThat(captor.getAllValues().get(1).getMessageRole()).isEqualTo(2);
    }

    @Test
    void chat_invalidConversationId_returnsForbidden() {
        when(userInfoRepository.findByUserId(1)).thenReturn(Optional.of(user));
        AiConversation otherConv = new AiConversation();
        otherConv.setId(99);
        UserInfo otherUser = new UserInfo();
        otherUser.setUserId(2);
        otherConv.setUser(otherUser);
        when(conversationRepository.findById(99)).thenReturn(Optional.of(otherConv));

        AiChatRequestDTO req = new AiChatRequestDTO();
        req.setMessage("hi");
        req.setConversationId(99);
        AiChatResponseDTO res = aiChatService.chat(1, req);

        assertThat(res.getReply()).contains("会话不存在或无权访问");
        assertThat(res.getConversationId()).isEqualTo(0);
    }
}
