package com.typinggame.admin.controller;

import com.typinggame.admin.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminProxyController {

    private final RestTemplate restTemplate;
    private final OperationLogService operationLogService;

    public AdminProxyController(RestTemplate adminRestTemplate, OperationLogService operationLogService) {
        this.restTemplate = adminRestTemplate;
        this.operationLogService = operationLogService;
    }

    @RequestMapping("/**")
    public ResponseEntity<String> proxy(HttpServletRequest request) throws IOException {
        long start = System.currentTimeMillis();
        String path = request.getRequestURI();
        String adminPrefix = request.getContextPath() + "/api/v1/admin";
        String adminPath = path.startsWith(adminPrefix) ? path.substring(adminPrefix.length()) : path;
        if (adminPath.isEmpty()) adminPath = "/";

        String target = AdminRoutes.resolveTargetService(adminPath);
        String query = request.getQueryString();
        String url = "http://" + target + AdminRoutes.contextPath(target) + "/api/v1/internal/admin" + adminPath
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

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, method, new HttpEntity<>(body.isEmpty() ? null : body, headers), String.class);
        } catch (HttpStatusCodeException e) {
            // 透传下游的业务错误（400/403/404 等）及其 message，而不是统一变成 500
            response = ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        }

        if (method != HttpMethod.GET && method != HttpMethod.OPTIONS && method != HttpMethod.HEAD) {
            operationLogService.record(currentUserId(), method.name(), "/api/v1/admin" + adminPath, query, body,
                    response.getStatusCode().value(), clientIp(request), System.currentTimeMillis() - start);
        }
        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    private static Integer currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        try {
            return Integer.parseInt(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
