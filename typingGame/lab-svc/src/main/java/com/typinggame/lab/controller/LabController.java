package com.typinggame.lab.controller;

import com.typinggame.lab.dto.AlgorithmView;
import com.typinggame.lab.dto.LabRunRequest;
import com.typinggame.lab.dto.LabRunView;
import com.typinggame.lab.dto.PresetRequest;
import com.typinggame.lab.dto.PresetView;
import com.typinggame.lab.service.LabCatalogService;
import com.typinggame.lab.service.LabRunService;
import com.typinggame.vo.ResultMessage;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/lab")
public class LabController {
    private final LabCatalogService catalog;
    private final LabRunService runs;

    public LabController(LabCatalogService catalog, LabRunService runs) {
        this.catalog = catalog;
        this.runs = runs;
    }

    @GetMapping("/algorithms")
    public ResponseEntity<ResultMessage<List<AlgorithmView>>> algorithms() {
        return ResponseEntity.ok(ResultMessage.success(catalog.publicAlgorithms()));
    }

    @GetMapping("/presets")
    public ResponseEntity<ResultMessage<List<PresetView>>> presets(@RequestParam(name = "algorithmId", required = false) String algorithmId) {
        return ResponseEntity.ok(ResultMessage.success(catalog.visiblePresets(algorithmId)));
    }

    @GetMapping("/presets/mine")
    public ResponseEntity<ResultMessage<List<PresetView>>> mine(@RequestParam(name = "algorithmId", required = false) String algorithmId) {
        return ResponseEntity.ok(ResultMessage.success(catalog.myPresets(currentUserId(), algorithmId)));
    }

    @PostMapping("/presets")
    public ResponseEntity<ResultMessage<PresetView>> create(@RequestBody PresetRequest request) {
        return ResponseEntity.ok(ResultMessage.success(catalog.createUserPreset(currentUserId(), request)));
    }

    @PutMapping("/presets/{id}")
    public ResponseEntity<ResultMessage<PresetView>> update(@PathVariable Long id, @RequestBody PresetRequest request) {
        return ResponseEntity.ok(ResultMessage.success(catalog.updateUserPreset(currentUserId(), id, request)));
    }

    @DeleteMapping("/presets/{id}")
    public ResponseEntity<ResultMessage<Void>> delete(@PathVariable Long id) {
        catalog.deleteUserPreset(currentUserId(), id);
        return ResponseEntity.ok(ResultMessage.success());
    }

    @PostMapping("/runs")
    public ResponseEntity<ResultMessage<LabRunView>> createRun(@RequestBody LabRunRequest request) {
        return ResponseEntity.ok(ResultMessage.success(runs.create(currentUserId(), request)));
    }

    @PatchMapping("/runs/{id}")
    public ResponseEntity<ResultMessage<LabRunView>> updateRun(@PathVariable Long id, @RequestBody LabRunRequest request) {
        return ResponseEntity.ok(ResultMessage.success(runs.update(currentUserId(), id, request)));
    }

    @GetMapping("/runs")
    public ResponseEntity<ResultMessage<Page<LabRunView>>> listRuns(
            @RequestParam(name = "algorithmId", required = false) String algorithmId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ResultMessage.success(runs.list(currentUserId(), algorithmId, page, size)));
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<ResultMessage<LabRunView>> getRun(@PathVariable Long id) {
        return ResponseEntity.ok(ResultMessage.success(runs.get(currentUserId(), id)));
    }

    @DeleteMapping("/runs/{id}")
    public ResponseEntity<ResultMessage<Void>> deleteRun(@PathVariable Long id) {
        runs.delete(currentUserId(), id);
        return ResponseEntity.ok(ResultMessage.success());
    }

    @PostMapping("/runs/{id}/fork-preset")
    public ResponseEntity<ResultMessage<PresetView>> forkPreset(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String name = body == null ? null : body.get("name");
        return ResponseEntity.ok(ResultMessage.success(runs.forkPreset(currentUserId(), id, name)));
    }

    private Integer currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("未登录");
        }
        return Integer.valueOf(String.valueOf(authentication.getPrincipal()));
    }
}
