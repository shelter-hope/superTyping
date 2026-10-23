package com.typinggame.lab.dto;

import java.util.Map;

public record PresetRequest(String algorithmId, String name, Map<String, Object> params, Boolean isPublic) {
}
