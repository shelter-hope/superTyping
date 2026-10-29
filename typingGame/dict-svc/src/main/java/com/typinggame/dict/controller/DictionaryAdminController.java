package com.typinggame.dict.controller;

import com.typinggame.dict.service.DictionaryAdminService;
import com.typinggame.dict.entity.CustomDictionary;
import com.typinggame.dict.entity.SystemDictionary;
import com.typinggame.dict.entity.SystemDictionaryWord;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/admin")
public class DictionaryAdminController {
    private final DictionaryAdminService dictionaryAdminService;

    public DictionaryAdminController(DictionaryAdminService dictionaryAdminService) {
        this.dictionaryAdminService = dictionaryAdminService;
    }

    @PostMapping("/system-dictionaries")
    public ResponseEntity<ResultMessage<SystemDictionary>> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ResultMessage.success(dictionaryAdminService.createSystemDictionary(body)));
    }

    @PutMapping("/system-dictionaries/{dictId}")
    public ResponseEntity<ResultMessage<SystemDictionary>> update(@PathVariable Integer dictId, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ResultMessage.success(dictionaryAdminService.updateSystemDictionary(dictId, body)));
    }

    @PatchMapping("/system-dictionaries/{dictId}/active")
    public ResponseEntity<ResultMessage<SystemDictionary>> active(@PathVariable Integer dictId, @RequestBody Map<String, Object> body) {
        boolean active = Boolean.TRUE.equals(body.get("isActive")) || "true".equals(String.valueOf(body.get("isActive")));
        return ResponseEntity.ok(ResultMessage.success(dictionaryAdminService.setSystemDictionaryActive(dictId, active)));
    }

    @DeleteMapping("/system-dictionaries/{dictId}")
    public ResponseEntity<ResultMessage<Void>> delete(@PathVariable Integer dictId) {
        dictionaryAdminService.deleteSystemDictionary(dictId);
        return ResponseEntity.ok(ResultMessage.success());
    }

    @PostMapping("/system-dictionaries/import")
    public ResponseEntity<ResultMessage<Map<String, Object>>> importDictionary(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ResultMessage.success(dictionaryAdminService.importSystemDictionary(body)));
    }

    @GetMapping("/system-dictionaries/{dictId}/export")
    public ResponseEntity<ResultMessage<Map<String, String>>> export(@PathVariable Integer dictId) {
        return ResponseEntity.ok(ResultMessage.success(Map.of("filename", "dictionary-" + dictId + ".csv", "content", dictionaryAdminService.exportWords(dictId))));
    }

    @PostMapping("/system-dictionary-words")
    public ResponseEntity<ResultMessage<SystemDictionaryWord>> createWord(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ResultMessage.success(dictionaryAdminService.createWord(body)));
    }

    @PutMapping("/system-dictionary-words/{wordId}")
    public ResponseEntity<ResultMessage<SystemDictionaryWord>> updateWord(@PathVariable Integer wordId, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ResultMessage.success(dictionaryAdminService.updateWord(wordId, body)));
    }

    @DeleteMapping("/system-dictionary-words/{wordId}")
    public ResponseEntity<ResultMessage<Void>> deleteWord(@PathVariable Integer wordId) {
        dictionaryAdminService.deleteWord(wordId);
        return ResponseEntity.ok(ResultMessage.success());
    }

    @PatchMapping("/custom-dictionaries/{dictId}/visibility")
    public ResponseEntity<ResultMessage<CustomDictionary>> visibility(@PathVariable Integer dictId, @RequestBody Map<String, Object> body) {
        boolean isPublic = Boolean.TRUE.equals(body.get("isPublic")) || "true".equals(String.valueOf(body.get("isPublic")));
        return ResponseEntity.ok(ResultMessage.success(dictionaryAdminService.setCustomVisibility(dictId, isPublic)));
    }
}
