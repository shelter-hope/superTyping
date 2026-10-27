package com.typinggame.lab.dto;

import java.util.Map;

public record LabRunRequest(
        String algorithmId,
        String pluginId,
        Integer schemaVersion,
        String title,
        String notes,
        Integer seed,
        Map<String, Object> params,
        Long presetId,
        String status,
        String startedAt,
        String endedAt,
        Integer durationMs,
        Map<String, Object> summary,
        Map<String, Object> metrics,
        Map<String, Object> clientMeta
) {
}
