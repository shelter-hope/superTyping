package com.typinggame.ai.service.impl;

import com.typinggame.ai.repository.AiChatMessageRepository;
import com.typinggame.ai.repository.AiConversationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalAdminServiceImplTest {

    @Mock
    private AiConversationRepository aiConversationRepository;

    @Mock
    private AiChatMessageRepository aiChatMessageRepository;

    @InjectMocks
    private InternalAdminServiceImpl internalAdminService;

    @Test
    void clampsPageAndPageSizeAndSortsByIdDesc() {
        when(aiConversationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        internalAdminService.getAiConversations(0, 999, 3, 1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(aiConversationRepository).findAll(any(Specification.class), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(200);
        Sort.Order order = pageable.getSort().getOrderFor("id");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void keepsValidPaging() {
        when(aiChatMessageRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        internalAdminService.getAiChatMessages(3, 20, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(aiChatMessageRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }
}
