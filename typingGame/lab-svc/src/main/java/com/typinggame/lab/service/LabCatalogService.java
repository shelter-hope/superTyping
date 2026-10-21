package com.typinggame.lab.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.lab.boids.BoidsLimits;
import com.typinggame.lab.config.LabNotFoundException;
import com.typinggame.lab.dto.AlgorithmUpdateRequest;
import com.typinggame.lab.dto.AlgorithmView;
import com.typinggame.lab.dto.PresetRequest;
import com.typinggame.lab.dto.PresetView;
import com.typinggame.lab.entity.LabAlgorithm;
import com.typinggame.lab.entity.LabPreset;
import com.typinggame.lab.repository.LabAlgorithmRepository;
import com.typinggame.lab.repository.LabPresetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class LabCatalogService {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };
    private static final String BOIDS_DEFAULTS = """
            {"count":80,"visionRadius":40,"visionAngle":3.141592653589793,"maxSpeed":2.2,"maxForce":0.05,"separation":1.4,"alignment":0.8,"cohesion":0.6}
            """;

    private final LabAlgorithmRepository algorithms;
    private final LabPresetRepository presets;
    private final ObjectMapper objectMapper;

    public LabCatalogService(LabAlgorithmRepository algorithms, LabPresetRepository presets, ObjectMapper objectMapper) {
        this.algorithms = algorithms;
        this.presets = presets;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ensureSeed() {
        algorithms.findById("boids").orElseGet(() -> {
            LabAlgorithm algorithm = new LabAlgorithm();
            algorithm.setId("boids");
            algorithm.setName("Boids 鸟群");
            algorithm.setDescription("分离、对齐和凝聚三条局部规则产生全局涌现。");
            algorithm.setEnabled(true);
            algorithm.setSortOrder(1);
            algorithm.setDefaultParams(BOIDS_DEFAULTS.trim());
            algorithm.setSchemaVersion(1);
            return algorithms.save(algorithm);
        });
        if (!presets.existsByUserIdIsNullAndAlgorithmIdAndName("boids", "课堂演示")) {
            LabPreset preset = new LabPreset();
            preset.setUserId(null);
            preset.setAlgorithmId("boids");
            preset.setName("课堂演示");
            preset.setIsPublic(true);
            preset.setParams("""
                    {"count":160,"visionRadius":48,"visionAngle":3.141592653589793,"maxSpeed":2.4,"maxForce":0.06,"separation":1.2,"alignment":1.1,"cohesion":0.8}
                    """.trim());
            presets.save(preset);
        }
    }

    public List<AlgorithmView> publicAlgorithms() {
        return algorithms.findByEnabledTrueOrderBySortOrderAscIdAsc().stream().map(this::toView).toList();
    }

    public List<AlgorithmView> allAlgorithms() {
        return algorithms.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toView).toList();
    }

    public List<PresetView> visiblePresets(String algorithmId) {
        return presets.findVisible(blankToNull(algorithmId)).stream().map(this::toView).toList();
    }

    public List<PresetView> myPresets(Integer userId, String algorithmId) {
        String key = blankToNull(algorithmId);
        List<LabPreset> rows = key == null
                ? presets.findByUserIdOrderByIdAsc(userId)
                : presets.findByUserIdAndAlgorithmIdOrderByIdAsc(userId, key);
        return rows.stream().map(this::toView).toList();
    }

    public List<PresetView> systemPresets() {
        return presets.findByUserIdIsNullOrderByIdAsc().stream().map(this::toView).toList();
    }

    @Transactional
    public PresetView createUserPreset(Integer userId, PresetRequest request) {
        return toView(savePreset(userId, request, false));
    }

    @Transactional
    public PresetView createSystemPreset(PresetRequest request) {
        PresetRequest publicRequest = new PresetRequest(request.algorithmId(), request.name(), request.params(), true);
        return toView(savePreset(null, publicRequest, true));
    }

    @Transactional
    public PresetView updateUserPreset(Integer userId, Long id, PresetRequest request) {
        LabPreset preset = presets.findById(id).orElseThrow(() -> new LabNotFoundException("预设不存在"));
        if (preset.getUserId() == null || !preset.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能修改自己的预设");
        }
        apply(preset, request, false);
        return toView(presets.save(preset));
    }

    @Transactional
    public void deleteUserPreset(Integer userId, Long id) {
        LabPreset preset = presets.findById(id).orElseThrow(() -> new LabNotFoundException("预设不存在"));
        if (preset.getUserId() == null || !preset.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能删除自己的预设");
        }
        presets.delete(preset);
    }

    @Transactional
    public void deleteSystemPreset(Long id) {
        LabPreset preset = presets.findById(id).orElseThrow(() -> new LabNotFoundException("预设不存在"));
        if (preset.getUserId() != null) {
            throw new IllegalArgumentException("只能删除系统预设");
        }
        presets.delete(preset);
    }

    @Transactional
    public AlgorithmView updateAlgorithm(String id, AlgorithmUpdateRequest request) {
        LabAlgorithm algorithm = algorithms.findById(id).orElseThrow(() -> new LabNotFoundException("算法不存在"));
        if (request.name() != null) {
            algorithm.setName(requireName(request.name()));
        }
        if (request.description() != null) {
            algorithm.setDescription(request.description());
        }
        if (request.enabled() != null) {
            algorithm.setEnabled(request.enabled());
        }
        if (request.sortOrder() != null) {
            algorithm.setSortOrder(request.sortOrder());
        }
        if (request.schemaVersion() != null) {
            algorithm.setSchemaVersion(request.schemaVersion());
        }
        if (request.defaultParams() != null) {
            validate(algorithm.getId(), request.defaultParams());
            algorithm.setDefaultParams(write(request.defaultParams()));
        }
        return toView(algorithms.save(algorithm));
    }

    private LabPreset savePreset(Integer userId, PresetRequest request, boolean system) {
        LabPreset preset = new LabPreset();
        preset.setUserId(userId);
        apply(preset, request, system);
        return presets.save(preset);
    }

    private void apply(LabPreset preset, PresetRequest request, boolean system) {
        LabAlgorithm algorithm = algorithms.findById(requireText(request.algorithmId(), "算法"))
                .orElseThrow(() -> new IllegalArgumentException("未知算法"));
        if (!Boolean.TRUE.equals(algorithm.getEnabled()) && !system) {
            throw new IllegalArgumentException("算法已停用");
        }
        validate(algorithm.getId(), request.params());
        preset.setAlgorithmId(algorithm.getId());
        preset.setName(requireName(request.name()));
        preset.setParams(write(request.params()));
        preset.setIsPublic(system || Boolean.TRUE.equals(request.isPublic()));
    }

    private void validate(String algorithmId, Map<String, Object> params) {
        if ("boids".equals(algorithmId)) {
            BoidsLimits.check(params);
            return;
        }
        throw new IllegalArgumentException("暂不支持该算法的参数校验");
    }

    private AlgorithmView toView(LabAlgorithm algorithm) {
        return new AlgorithmView(
                algorithm.getId(),
                algorithm.getName(),
                algorithm.getDescription(),
                Boolean.TRUE.equals(algorithm.getEnabled()),
                algorithm.getSortOrder() == null ? 0 : algorithm.getSortOrder(),
                read(algorithm.getDefaultParams()),
                algorithm.getSchemaVersion() == null ? 1 : algorithm.getSchemaVersion()
        );
    }

    private PresetView toView(LabPreset preset) {
        return new PresetView(
                preset.getId(),
                preset.getUserId(),
                preset.getAlgorithmId(),
                preset.getName(),
                read(preset.getParams()),
                Boolean.TRUE.equals(preset.getIsPublic()),
                preset.getUserId() == null
        );
    }

    private Map<String, Object> read(String json) {
        try {
            return objectMapper.readValue(json, MAP);
        } catch (Exception ex) {
            throw new IllegalArgumentException("参数 JSON 无法解析");
        }
    }

    private String write(Map<String, Object> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception ex) {
            throw new IllegalArgumentException("参数无法序列化");
        }
    }

    private static String requireName(String name) {
        String trimmed = requireText(name, "名称");
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("名称不能超过 100 字");
        }
        return trimmed;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
