package com.typinggame.lab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "lab_run")
public class LabRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "algorithm_id", nullable = false, length = 64)
    private String algorithmId;

    @Column(name = "plugin_id", nullable = false, length = 64)
    private String pluginId;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion = 1;

    @Column(name = "engine_kind", nullable = false, length = 32)
    private String engineKind = "client_snapshot";

    @Column(length = 120)
    private String title;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private Integer seed;

    @Column(name = "params_json", nullable = false, columnDefinition = "json")
    private String paramsJson;

    @Column(name = "preset_id")
    private Long presetId;

    @Column(nullable = false, length = 16)
    private String status = "completed";

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "summary_json", columnDefinition = "json")
    private String summaryJson;

    @Column(name = "metrics_json", columnDefinition = "json")
    private String metricsJson;

    @Column(name = "client_meta_json", columnDefinition = "json")
    private String clientMetaJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
