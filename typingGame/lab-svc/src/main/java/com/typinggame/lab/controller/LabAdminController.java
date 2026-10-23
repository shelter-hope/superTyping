package com.typinggame.lab.controller;

import com.typinggame.lab.dto.AlgorithmUpdateRequest;
import com.typinggame.lab.dto.AlgorithmView;
import com.typinggame.lab.dto.PresetRequest;
import com.typinggame.lab.dto.PresetView;
import com.typinggame.lab.service.LabCatalogService;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/admin/lab")
public class LabAdminController {
    private final LabCatalogService catalog;

    public LabAdminController(LabCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/algorithms")
    public ResponseEntity<ResultMessage<List<AlgorithmView>>> algorithms() {
        return ResponseEntity.ok(ResultMessage.success(catalog.allAlgorithms()));
    }

    @PutMapping("/algorithms/{id}")
    public ResponseEntity<ResultMessage<AlgorithmView>> update(@PathVariable String id, @RequestBody AlgorithmUpdateRequest request) {
        return ResponseEntity.ok(ResultMessage.success(catalog.updateAlgorithm(id, request)));
    }

    @GetMapping("/presets")
    public ResponseEntity<ResultMessage<List<PresetView>>> presets() {
        return ResponseEntity.ok(ResultMessage.success(catalog.systemPresets()));
    }

    @PostMapping("/presets")
    public ResponseEntity<ResultMessage<PresetView>> create(@RequestBody PresetRequest request) {
        return ResponseEntity.ok(ResultMessage.success(catalog.createSystemPreset(request)));
    }

    @DeleteMapping("/presets/{id}")
    public ResponseEntity<ResultMessage<Void>> delete(@PathVariable Long id) {
        catalog.deleteSystemPreset(id);
        return ResponseEntity.ok(ResultMessage.success());
    }
}
