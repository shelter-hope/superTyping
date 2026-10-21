package com.typinggame.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 后台操作日志（对应 admin_operation_log 表），由 admin-svc 代理层对所有写操作自动记录
 */
@Entity
@Table(name = "admin_operation_log")
@Data
@NoArgsConstructor
public class AdminOperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id")
    private Integer operatorId;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(name = "query_string", length = 500)
    private String queryString;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(nullable = false)
    private Boolean success;

    @Column(length = 64)
    private String ip;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
}
