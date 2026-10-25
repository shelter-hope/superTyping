package com.typinggame.admin.controller;

import com.typinggame.admin.service.OperationLogService;
import com.typinggame.vo.ResultMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 由 admin-svc 自己处理（不转发）的后台接口：数据概览、操作日志。
 * 路径比代理的 /** 更具体，优先匹配。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
public class AdminLocalController {

    private static final Map<String, String> OVERVIEW_SOURCES = Map.of(
            "users", "user-svc",
            "games", "game-core-svc",
            "chat", "chat-svc",
            "dictionaries", "dict-svc");

    private final RestTemplate restTemplate;
    private final OperationLogService operationLogService;

    public AdminLocalController(RestTemplate adminRestTemplate, OperationLogService operationLogService) {
        this.restTemplate = adminRestTemplate;
        this.operationLogService = operationLogService;
    }

    /** 汇总各服务的统计；某个服务不可用时该部分为 null，不影响其他部分 */
    @GetMapping("/overview")
    public ResponseEntity<ResultMessage<Map<String, Object>>> overview(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null) headers.set(HttpHeaders.AUTHORIZATION, auth);

        Map<String, Object> result = new LinkedHashMap<>();
        OVERVIEW_SOURCES.forEach((section, service) -> result.put(section, fetchSection(section, service, headers)));
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/operation-logs")
    public ResponseEntity<ResultMessage<Map<String, Object>>> operationLogs(@RequestParam(defaultValue = "1") int page,
                                                                            @RequestParam(defaultValue = "20") int pageSize,
                                                                            @RequestParam(required = false) Integer operatorId,
                                                                            @RequestParam(required = false) String method,
                                                                            @RequestParam(required = false) String path,
                                                                            @RequestParam(required = false) Boolean success) {
        return ResponseEntity.ok(ResultMessage.success(operationLogService.list(page, pageSize, operatorId, method, path, success)));
    }

    private Object fetchSection(String section, String service, HttpHeaders headers) {
        String url = "http://" + service + AdminRoutes.contextPath(service) + "/api/v1/internal/admin/overview/" + section;
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() { });
            Map<String, Object> body = resp.getBody();
            return body == null ? null : body.get("data");
        } catch (RuntimeException e) {
            log.warn("获取概览数据失败: {}", url, e);
            return null;
        }
    }
}
