package com.typinggame.chat.controller;

import com.typinggame.chat.service.DictionaryService;
import com.typinggame.dto.dictionary.DictionaryCreateDTO;
import com.typinggame.entity.dictionaries.CustomDictionary;
import com.typinggame.entity.dictionaries.SystemDictionary;
import com.typinggame.vo.ResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private DictionaryService dictionaryService;

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