package com.typinggame.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminProxyController {

    private final RestTemplate restTemplate;

    public AdminProxyController(RestTemplate adminRestTemplate) {
        this.restTemplate = adminRestTemplate;
    }

    @RequestMapping("/**")
    public ResponseEntity<String> proxy(HttpServletRequest request) throws IOException {
        String path = request.getRequestURI();
        String adminPrefix = request.getContextPath() + "/api/v1/admin";
        String adminPath = path.startsWith(adminPrefix) ? path.substring(adminPrefix.length()) : path;
        if (adminPath.isEmpty()) adminPath = "/";

        String target = resolveTargetService(adminPath);
        String query = request.getQueryString();
        String targetContextPath = resolveTargetContextPath(target);
        String url = "http://" + target + targetContextPath + "/api/v1/internal/admin" + adminPath
                + (query == null ? "" : "?" + query);

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!"host".equalsIgnoreCase(name) && !"content-length".equalsIgnoreCase(name)) {
                headers.add(name, request.getHeader(name));
            }
        }
        String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        return restTemplate.exchange(url, method, new HttpEntity<>(body.isEmpty() ? null : body, headers), String.class);
    }

    private String resolveTargetContextPath(String service) {
        return switch (service) {
            case "user-svc" -> "/user-svc";
            case "game-core-svc" -> "/game-core-svc";
            case "chat-svc" -> "/chat-svc";
            case "ai-svc" -> "/ai-svc";
            default -> throw new IllegalArgumentException("不支持的后台目标服务: " + service);
        };
    }

    private String resolveTargetService(String path) {
        if (path.matches("/users/\\d+/game-data")) {
            return "game-core-svc";
        }
        if (path.startsWith("/users")
                || path.startsWith("/friend-relations")
                || path.startsWith("/user-app-configs")
                || path.startsWith("/user-game-mode-configs")
                || path.startsWith("/user-monster-configs")
                || path.startsWith("/user-model-params")
                || path.startsWith("/user-daily-stats")
                || path.startsWith("/user-info-snapshots")) {
            return "user-svc";
        }
        if (path.startsWith("/system-dictionaries")
                || path.startsWith("/system-dictionary-words")
                || path.startsWith("/custom-dictionaries")
                || path.startsWith("/custom-dictionary-words")
                || path.startsWith("/leaderboard-entries")
                || path.startsWith("/user-conversations")
                || path.startsWith("/friend-chat-messages")
                || path.startsWith("/system-notifications")) {
            return "chat-svc";
        }
        if (path.startsWith("/ai-conversations") || path.startsWith("/ai-chat-messages")) {
            return "ai-svc";
        }
        return "game-core-svc";
    }
}
