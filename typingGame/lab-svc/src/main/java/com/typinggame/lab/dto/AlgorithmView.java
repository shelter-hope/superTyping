package com.typinggame.lab.dto;

import java.util.Map;

public record AlgorithmView(
        String id,
        String name,
        String description,
        boolean enabled,
        int sortOrder,
        Map<String, Object> defaultParams,
        int schemaVersion
) {
}
