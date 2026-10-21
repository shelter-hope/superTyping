package com.typinggame.chat.im;

import com.typinggame.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImHandshakeInterceptorTest {

    private JwtTokenProvider jwtTokenProvider;
    private ImHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        interceptor = new ImHandshakeInterceptor(jwtTokenProvider);
    }

    @Test
    void validTokenPutsUserIdIntoSessionAttributes() {
        when(jwtTokenProvider.validateToken("good")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("good")).thenReturn(7);
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = handshake("token=good", new MockHttpServletResponse(), attributes);

        assertTrue(accepted);
        assertEquals(7, attributes.get(ImHandshakeInterceptor.USER_ID_ATTR));
    }

    @Test
    void missingTokenIsRejectedWith401() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(handshake(null, response, new HashMap<>()));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }

    @Test
    void invalidTokenIsRejected() {
        when(jwtTokenProvider.validateToken("bad")).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(handshake("token=bad", response, new HashMap<>()));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }

    private boolean handshake(String query, MockHttpServletResponse servletResponse, Map<String, Object> attributes) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/im");
        servletRequest.setQueryString(query);
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest), response, mock(WebSocketHandler.class), attributes);
        try {
            response.flush();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return accepted;
    }
}
