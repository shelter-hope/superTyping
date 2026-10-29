package com.typinggame.user.service.impl;

import com.typinggame.common.exception.BusinessException;
import com.typinggame.common.security.JwtTokenProvider;
import com.typinggame.common.security.TokenRevocationService;
import com.typinggame.dto.auth.UserLoginDTO;
import com.typinggame.user.entity.UserInfo;
import com.typinggame.user.repository.UserInfoRepository;
import com.typinggame.user.service.UserRegistrationInitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private UserInfoRepository userInfoRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private AuthServiceImpl service;
    private UserInfo user;

    @BeforeEach
    void setUp() {
        userInfoRepository = mock(UserInfoRepository.class);
        passwordEncoder = mock(BCryptPasswordEncoder.class);
        service = new AuthServiceImpl(userInfoRepository, passwordEncoder, mock(JwtTokenProvider.class),
                mock(TokenRevocationService.class), mock(UserRegistrationInitService.class));

        user = new UserInfo();
        user.setUserId(5);
        user.setUsername("alice");
        user.setPassword("hash");
        user.setRole("user");
        when(userInfoRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
    }

    private static UserLoginDTO login() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("alice");
        dto.setPassword("secret");
        return dto;
    }

    @Test
    void disabledUserCannotLogin() {
        user.setStatus(UserInfo.STATUS_DISABLED);

        BusinessException e = assertThrows(BusinessException.class, () -> service.login(login()));

        assertEquals(403, e.getCode());
        verify(userInfoRepository, never()).save(any());
    }

    @Test
    void successfulLoginRecordsLastLoginTime() {
        user.setStatus(null);

        service.login(login());

        assertNotNull(user.getLastLoginTime());
        verify(userInfoRepository).save(user);
    }
}
