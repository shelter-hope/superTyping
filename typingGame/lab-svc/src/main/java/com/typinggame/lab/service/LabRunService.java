package com.typinggame.lab.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.lab.boids.ParamLimits;
import com.typinggame.lab.config.LabNotFoundException;
import com.typinggame.lab.dto.LabRunRequest;
import com.typinggame.lab.dto.LabRunView;
import com.typinggame.lab.dto.PresetRequest;
import com.typinggame.lab.dto.PresetView;
import com.typinggame.lab.entity.LabAlgorithm;
import com.typinggame.lab.entity.LabRun;
import com.typinggame.lab.repository.LabAlgorithmRepository;
import com.typinggame.lab.repository.LabRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LabRunService {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };
    private static final Set<String> STATUSES = Set.of("running", "completed", "aborted");
    private static final int MAX_METRICS_CHARS = 240_000;
    private static final String ENGINE = "client_snapshot";

    private final LabRunRepository runs;
    private final LabAlgorithmRepository algorithms;
    private final LabCatalogService catalog;
    private final ObjectMapper objectMapper;

    public LabRunService(LabRunRepository runs,
                         LabAlgorithmRepository algorithms,
                         LabCatalogService catalog,
                         ObjectMapper objectMapper) {
        this.runs = runs;
        this.algorithms = algorithms;
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LabRunView create(Integer userId, LabRunRequest request) {
        LabRun run = new LabRun();
        run.setUserId(userId);
        run.setCreatedAt(LocalDateTime.now());
        apply(run, request, true);
        return toView(runs.save(run), true);
    }

    @Transactional
    public LabRunView update(Integer userId, Long id, LabRunRequest request) {
        LabRun run = runs.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new LabNotFoundException("实验记录不存在"));
        apply(run, request, false);
        return toView(runs.save(run), true);
    }

    @Transactional(readOnly = true)
    public Page<LabRunView> list(Integer userId, String algorithmId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<LabRun> rows = algorithmId == null || algorithmId.isBlank()
                ? runs.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : runs.findByUserIdAndAlgorithmIdOrderByCreatedAtDesc(userId, algorithmId.trim(), pageable);
        return rows.map(run -> toView(run, false));
    }

    @Transactional(readOnly = true)
    public LabRunView get(Integer userId, Long id) {
        LabRun run = runs.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new LabNotFoundException("实验记录不存在"));
        return toView(run, true);
    }

    @Transactional
    public void delete(Integer userId, Long id) {
        LabRun run = runs.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new LabNotFoundException("实验记录不存在"));
        runs.delete(run);
    }

    @Transactional
    public PresetView forkPreset(Integer userId, Long id, String name) {
        LabRun run = runs.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new LabNotFoundException("实验记录不存在"));
        Map<String, Object> params = readMap(run.getParamsJson());
        String presetName = (name == null || name.isBlank())
                ? (run.getTitle() == null || run.getTitle().isBlank() ? "来自实验 " + id : run.getTitle())
                : name.trim();
        return catalog.createUserPreset(userId, new PresetRequest(run.getAlgorithmId(), presetName, params, false));
    }

    private void apply(LabRun run, LabRunRequest request, boolean creating) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        String algorithmId = requireText(request.algorithmId(), "algorithmId");
        LabAlgorithm algorithm = algorithms.findById(algorithmId)
                .orElseThrow(() -> new IllegalArgumentException("未知算法: " + algorithmId));
        if (creating || request.algorithmId() != null) {
            run.setAlgorithmId(algorithmId);
        }
        String pluginId = request.pluginId() == null || request.pluginId().isBlank()
                ? algorithmId
                : request.pluginId().trim();
        run.setPluginId(pluginId);
        int schemaVersion = request.schemaVersion() == null ? algorithm.getSchemaVersion() : request.schemaVersion();
        run.setSchemaVersion(schemaVersion);
        run.setEngineKind(ENGINE);
        if (request.title() != null) {
            run.setTitle(trimTo(request.title(), 120));
        }
        if (request.notes() != null) {
            run.setNotes(trimTo(request.notes(), 1000));
        }
        if (creating || request.seed() != null) {
            if (request.seed() == null) {
                throw new IllegalArgumentException("seed 必填");
            }
            run.setSeed(request.seed());
        }
        if (creating || request.params() != null) {
            if (request.params() == null || request.params().isEmpty()) {
                throw new IllegalArgumentException("params 必填");
            }
            ParamLimits.check(algorithmId, request.params());
            run.setParamsJson(writeJson(request.params()));
        }
        if (request.presetId() != null) {
            run.setPresetId(request.presetId());
        }
        String status = request.status() == null || request.status().isBlank()
                ? (creating ? "completed" : run.getStatus())
                : request.status().trim().toLowerCase();
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException("status 必须是 running/completed/aborted");
        }
        run.setStatus(status);
        if (request.startedAt() != null) {
            run.setStartedAt(parseTime(request.startedAt()));
        } else if (creating && run.getStartedAt() == null) {
            run.setStartedAt(LocalDateTime.now());
        }
        if (request.endedAt() != null) {
            run.setEndedAt(parseTime(request.endedAt()));
        } else if (creating && !"running".equals(status) && run.getEndedAt() == null) {
            run.setEndedAt(LocalDateTime.now());
        }
        if (request.durationMs() != null) {
            if (request.durationMs() < 0) {
                throw new IllegalArgumentException("durationMs 不能为负");
            }
            run.setDurationMs(request.durationMs());
        }
        if (request.summary() != null) {
            run.setSummaryJson(writeJson(request.summary()));
        }
        if (request.metrics() != null) {
            String metrics = writeJson(sanitizeMetrics(request.metrics()));
            if (metrics.length() > MAX_METRICS_CHARS) {
                throw new IllegalArgumentException("metrics 过大，请先降采样");
            }
            run.setMetricsJson(metrics);
        }
        if (request.clientMeta() != null) {
            run.setClientMetaJson(writeJson(request.clientMeta()));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeMetrics(Map<String, Object> metrics) {
        Map<String, Object> copy = new LinkedHashMap<>(metrics);
        Object maxPoints = copy.get("maxPoints");
        if (!(maxPoints instanceof Number)) {
            copy.put("maxPoints", 240);
        }
        Object seriesObj = copy.get("series");
        if (seriesObj instanceof Map<?, ?> series) {
            Map<String, Object> trimmed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : series.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                Object value = entry.getValue();
                if (value instanceof List<?> list && list.size() > 240) {
                    trimmed.put(String.valueOf(entry.getKey()), list.subList(list.size() - 240, list.size()));
                } else {
                    trimmed.put(String.valueOf(entry.getKey()), value);
                }
            }
            copy.put("series", trimmed);
        }
        Object times = copy.get("t");
        if (times instanceof List<?> list && list.size() > 240) {
            copy.put("t", list.subList(list.size() - 240, list.size()));
        }
        return copy;
    }

    private LabRunView toView(LabRun run, boolean includeMetrics) {
        Integer catalogVersion = algorithms.findById(run.getAlgorithmId())
                .map(LabAlgorithm::getSchemaVersion)
                .orElse(run.getSchemaVersion());
        boolean stale = catalogVersion != null && !catalogVersion.equals(run.getSchemaVersion());
        return new LabRunView(
                run.getId(),
                run.getUserId(),
                run.getAlgorithmId(),
                run.getPluginId(),
                run.getSchemaVersion(),
                stale,
                run.getEngineKind(),
                run.getTitle(),
                run.getNotes(),
                run.getSeed(),
                readMap(run.getParamsJson()),
                run.getPresetId(),
                run.getStatus(),
                formatTime(run.getStartedAt()),
                formatTime(run.getEndedAt()),
                run.getDurationMs(),
                readMap(run.getSummaryJson()),
                includeMetrics ? readMap(run.getMetricsJson()) : null,
                readMap(run.getClientMetaJson()),
                formatTime(run.getCreatedAt())
        );
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP);
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("无法序列化 JSON");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 必填");
        }
        return value.trim();
    }

    private static String trimTo(String value, int max) {
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim().replace(" ", "T").replace("Z", ""));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("时间格式无效: " + value);
        }
    }

    private static String formatTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
