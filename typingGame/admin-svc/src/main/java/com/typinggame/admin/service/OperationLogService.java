package com.typinggame.admin.service;

import com.typinggame.admin.entity.AdminOperationLog;
import com.typinggame.admin.repository.AdminOperationLogRepository;
import com.typinggame.common.admin.AdminFilter;
import com.typinggame.common.admin.AdminPageSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class OperationLogService {

    static final int MAX_BODY = 2000;

    private final AdminOperationLogRepository repository;

    public OperationLogService(AdminOperationLogRepository repository) {
        this.repository = repository;
    }

    /** 记录失败只打日志，不能影响后台操作本身 */
    public void record(Integer operatorId, String method, String path, String query, String body,
                       int status, String ip, long durationMs) {
        try {
            AdminOperationLog entry = new AdminOperationLog();
            entry.setOperatorId(operatorId);
            entry.setMethod(method);
            entry.setPath(truncate(path, 255));
            entry.setQueryString(truncate(query, 500));
            entry.setRequestBody(truncate(body, MAX_BODY));
            entry.setResponseStatus(status);
            entry.setSuccess(status >= 200 && status < 300);
            entry.setIp(truncate(ip, 64));
            entry.setDurationMs((int) Math.min(Integer.MAX_VALUE, durationMs));
            entry.setCreateTime(LocalDateTime.now());
            repository.save(entry);
        } catch (RuntimeException e) {
            log.warn("记录后台操作日志失败: {} {}", method, path, e);
        }
    }

    public Map<String, Object> list(int page, int pageSize, Integer operatorId, String method, String path, Boolean success) {
        AdminFilter<AdminOperationLog> f = AdminFilter.of(AdminOperationLog.class)
                .eq("operatorId", operatorId)
                .eq("method", method == null ? null : method.toUpperCase())
                .like("path", path)
                .eq("success", success);
        PageRequest pr = PageRequest.of(Math.max(0, page - 1), Math.min(200, Math.max(1, pageSize)), Sort.by(Sort.Direction.DESC, "id"));
        return AdminPageSupport.pageMap("items", repository.findAll(f, pr));
    }

    static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
