package com.typinggame.lab.dto;

import java.util.Map;

public record PresetView(
        Long id,
        Integer userId,
        String algorithmId,
        String name,
        Map<String, Object> params,
        boolean isPublic,
        boolean system
) {
}
