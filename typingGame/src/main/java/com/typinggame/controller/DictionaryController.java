package com.typinggame.controller;

import com.typinggame.dto.dictionary.DictionaryCreateDTO;
import com.typinggame.entity.dictionaries.CustomDictionary;
import com.typinggame.entity.dictionaries.SystemDictionary;
import com.typinggame.service.DictionaryService;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 词库管理 API
 */
@RestController
@Tag(name = "词库管理 API")
@RequestMapping("/api/v1/dictionaries")
public class DictionaryController {

    @Autowired
    private DictionaryService dictionaryService;

    /**
     * 获取系统词库
     * @return 系统词库列表
     */
    @GetMapping("/system")
    @Operation(summary = "获取系统词库")
    public ResponseEntity<ResultMessage<List<SystemDictionary>>> getSystemDictionaries() {
        List<SystemDictionary> dicts = dictionaryService.getSystemDictionaries();
        return ResponseEntity.ok(ResultMessage.success(dicts));
    }

    /**
     * 获取我的词库
     * @return 自定义词库列表
     */
    @GetMapping("/custom")
    @Operation(summary = "获取我的词库")
    public ResponseEntity<ResultMessage<List<CustomDictionary>>> getUserDictionaries() {
        Integer userId = getCurrentUserId();
        List<CustomDictionary> dicts = dictionaryService.getUserDictionaries(userId);
        return ResponseEntity.ok(ResultMessage.success(dicts));
    }

    /**
     * 创建自定义词库 (支持文件上传)
     * @param name 词库名称
     * @param description 描述
     * @param isPublic 是否公开
     * @param file 词库文件
     * @return 创建的词库
     */
    @PostMapping(value = "/custom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "创建自定义词库")
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

    /**
     * 删除自定义词库
     * @param id 词库ID
     * @return 是否成功
     */
    @DeleteMapping("/custom/{id}")
    @Operation(summary = "删除自定义词库")
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
