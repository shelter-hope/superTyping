package com.typinggame.lab.dto;

import java.util.Map;

public record AlgorithmUpdateRequest(
        String name,
        String description,
        Boolean enabled,
        Integer sortOrder,
        Map<String, Object> defaultParams,
        Integer schemaVersion
) {
}
