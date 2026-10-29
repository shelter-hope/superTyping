package com.typinggame.dict.controller;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.typinggame.dict.service.InternalAdminService;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "internal", description = "服务间内部接口（网关对 /internal/ 返回 404）")
@RestController
@RequestMapping("/api/v1/internal/admin")
public class InternalAdminController {
    private final InternalAdminService internalAdminService;

    public InternalAdminController(InternalAdminService internalAdminService) {
        this.internalAdminService = internalAdminService;
    }

    @GetMapping("/system-dictionaries")
    public ResponseEntity<ResultMessage<Map<String, Object>>> systemDictionaries(@RequestParam(defaultValue = "1") int page,
                                                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                                                 @RequestParam(required = false) String name,
                                                                                 @RequestParam(required = false) String category,
                                                                                 @RequestParam(required = false) Boolean isActive) {
        return ok(internalAdminService.getSystemDictionaries(page, pageSize, name, category, isActive));
    }

    @GetMapping("/custom-dictionaries")
    public ResponseEntity<ResultMessage<Map<String, Object>>> customDictionaries(@RequestParam(defaultValue = "1") int page,
                                                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                                                 @RequestParam(required = false) Integer userId,
                                                                                 @RequestParam(required = false) String name,
                                                                                 @RequestParam(required = false) Boolean isPublic) {
        return ok(internalAdminService.getCustomDictionaries(page, pageSize, userId, name, isPublic));
    }

    @GetMapping("/system-dictionary-words")
    public ResponseEntity<ResultMessage<Map<String, Object>>> systemWords(@RequestParam Integer dictId,
                                                                          @RequestParam(defaultValue = "1") int page,
                                                                          @RequestParam(defaultValue = "50") int pageSize) {
        return ok(internalAdminService.getSystemDictionaryWords(dictId, page, pageSize));
    }

    @GetMapping("/custom-dictionary-words")
    public ResponseEntity<ResultMessage<Map<String, Object>>> customWords(@RequestParam Integer dictId,
                                                                          @RequestParam(defaultValue = "1") int page,
                                                                          @RequestParam(defaultValue = "50") int pageSize) {
        return ok(internalAdminService.getCustomDictionaryWords(dictId, page, pageSize));
    }

    @GetMapping("/overview/dictionaries")
    public ResponseEntity<ResultMessage<Map<String, Object>>> overview() {
        return ok(internalAdminService.getOverview());
    }

    private ResponseEntity<ResultMessage<Map<String, Object>>> ok(Map<String, Object> data) {
        return ResponseEntity.ok(ResultMessage.success(data));
    }
}
