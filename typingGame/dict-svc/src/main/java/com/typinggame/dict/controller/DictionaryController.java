package com.typinggame.dict.controller;

import com.typinggame.dict.support.dictionary.DictionaryFileStore;
import com.typinggame.dict.service.DictionaryService;
import com.typinggame.dto.dictionary.DictionaryCreateDTO;
import com.typinggame.dict.entity.CustomDictionary;
import com.typinggame.dict.entity.SystemDictionary;
import com.typinggame.vo.ResultMessage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dictionaries")
public class DictionaryController {

    private final DictionaryService dictionaryService;
    private final DictionaryFileStore dictionaryFileStore;

    public DictionaryController(DictionaryService dictionaryService, DictionaryFileStore dictionaryFileStore) {
        this.dictionaryService = dictionaryService;
        this.dictionaryFileStore = dictionaryFileStore;
    }

    @GetMapping("/files/{name}")
    public ResponseEntity<Resource> file(@PathVariable String name) {
        FileSystemResource resource = new FileSystemResource(dictionaryFileStore.resolve(name));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + "\"")
                .contentType(name.endsWith(".csv") ? MediaType.parseMediaType("text/csv") : MediaType.TEXT_PLAIN)
                .body(resource);
    }

    @GetMapping("/system")
    public ResponseEntity<ResultMessage<List<SystemDictionary>>> getSystemDictionaries() {
        List<SystemDictionary> dicts = dictionaryService.getSystemDictionaries();
        return ResponseEntity.ok(ResultMessage.success(dicts));
    }

    @GetMapping("/custom")
    public ResponseEntity<ResultMessage<List<CustomDictionary>>> getUserDictionaries() {
        Integer userId = getCurrentUserId();
        List<CustomDictionary> dicts = dictionaryService.getUserDictionaries(userId);
        return ResponseEntity.ok(ResultMessage.success(dicts));
    }

    @PostMapping(value = "/custom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultMessage<CustomDictionary>> createCustomDictionary(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "isPublic", defaultValue = "false") Boolean isPublic,
            @RequestParam("file") MultipartFile file) {

        Integer userId = getCurrentUserId();
        DictionaryCreateDTO dto = new DictionaryCreateDTO();
        dto.setName(name);
        dto.setDescription(description);
        dto.setIsPublic(isPublic);

        CustomDictionary dict = dictionaryService.createCustomDictionary(userId, dto, file);
        return ResponseEntity.ok(ResultMessage.success(dict));
    }

    @DeleteMapping("/custom/{id}")
    public ResponseEntity<ResultMessage<Boolean>> deleteCustomDictionary(@PathVariable("id") Integer id) {
        Integer userId = getCurrentUserId();
        boolean result = dictionaryService.deleteCustomDictionary(userId, id);
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}