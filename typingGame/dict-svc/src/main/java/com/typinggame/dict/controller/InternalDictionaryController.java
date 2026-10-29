package com.typinggame.dict.controller;

import com.typinggame.dict.service.InternalDictionaryService;
import com.typinggame.dto.dictionary.DictionaryIdsQueryDTO;
import com.typinggame.dto.dictionary.DictionaryPracticeUpdateDTO;
import com.typinggame.dto.dictionary.DictionaryWordItemDTO;
import com.typinggame.dto.dictionary.DictionaryWordsQueryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 词库服务的内部接口：只允许集群内其它服务调用（网关对 /internal/ 路径直接 404）。
 */
@RestController
@RequestMapping("/api/v1/internal/dictionaries")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InternalDictionaryController {

    private final InternalDictionaryService internalDictionaryService;

    @PostMapping("/ids")
    public List<Integer> ids(@RequestBody DictionaryIdsQueryDTO query) {
        if ("SYSTEM".equalsIgnoreCase(query.getSource())) {
            return internalDictionaryService.activeSystemDictIds(query.getDictIds());
        }
        return internalDictionaryService.allowedCustomDictIds(query.getDictIds(), query.getUserId());
    }

    @PostMapping("/words")
    public List<DictionaryWordItemDTO> words(@RequestBody DictionaryWordsQueryDTO query) {
        return internalDictionaryService.words(query.getSource(), query.getDictIds(),
                query.getWords() == null ? null : new LinkedHashSet<>(query.getWords()), query.getLimit());
    }

    @PostMapping("/practice")
    public Map<String, Object> practice(@RequestBody DictionaryPracticeUpdateDTO update) {
        int updated = internalDictionaryService.updatePractice(update);
        return Map.of("updated", updated);
    }
}
