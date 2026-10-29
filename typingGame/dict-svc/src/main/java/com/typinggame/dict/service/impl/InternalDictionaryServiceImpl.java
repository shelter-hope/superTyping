package com.typinggame.dict.service.impl;

import com.typinggame.dict.repository.CustomDictionaryRepository;
import com.typinggame.dict.repository.DictionaryWordRepository;
import com.typinggame.dict.repository.SystemDictionaryRepository;
import com.typinggame.dict.repository.SystemDictionaryWordRepository;
import com.typinggame.dict.service.InternalDictionaryService;
import com.typinggame.dto.dictionary.DictionaryPracticeUpdateDTO;
import com.typinggame.dto.dictionary.DictionaryWordItemDTO;
import com.typinggame.dict.entity.CustomDictionary;
import com.typinggame.dict.entity.DictionaryWord;
import com.typinggame.dict.entity.SystemDictionary;
import com.typinggame.dict.entity.SystemDictionaryWord;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InternalDictionaryServiceImpl implements InternalDictionaryService {
    private static final Logger log = LoggerFactory.getLogger(InternalDictionaryServiceImpl.class);
    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 2000;
    private static final int MAX_PRACTICE_ITEMS = 500;

    private final CustomDictionaryRepository customDictionaryRepository;
    private final SystemDictionaryRepository systemDictionaryRepository;
    private final DictionaryWordRepository dictionaryWordRepository;
    private final SystemDictionaryWordRepository systemDictionaryWordRepository;

    @Override
    public List<Integer> allowedCustomDictIds(Collection<Integer> dictIds, Integer userId) {
        if (dictIds == null || dictIds.isEmpty() || userId == null) {
            return List.of();
        }
        return customDictionaryRepository.findByDictIdInAndUserId(dictIds, userId).stream()
                .map(CustomDictionary::getDictId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> activeSystemDictIds(Collection<Integer> dictIds) {
        if (dictIds == null || dictIds.isEmpty()) {
            return List.of();
        }
        return systemDictionaryRepository.findByDictIdInAndIsActiveTrue(dictIds).stream()
                .map(SystemDictionary::getDictId)
                .collect(Collectors.toList());
    }

    @Override
    public List<DictionaryWordItemDTO> words(String source, Collection<Integer> dictIds, Collection<String> words, Integer limit) {
        if (dictIds == null || dictIds.isEmpty()) {
            return List.of();
        }
        int size = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        if ("SYSTEM".equalsIgnoreCase(source)) {
            return systemDictionaryWordRepository.findByDictIdIn(dictIds, PageRequest.of(0, size)).stream()
                    .map(word -> new DictionaryWordItemDTO(word.getWord(), word.getDifficultyLevel()))
                    .collect(Collectors.toList());
        }
        if (words != null && !words.isEmpty()) {
            return dictionaryWordRepository.findByDictIdInAndWordIn(dictIds, words).stream()
                    .map(word -> new DictionaryWordItemDTO(word.getWord(), word.getDifficultyLevel()))
                    .collect(Collectors.toList());
        }
        return dictionaryWordRepository.findByDictIdIn(dictIds, PageRequest.of(0, size)).stream()
                .map(word -> new DictionaryWordItemDTO(word.getWord(), word.getDifficultyLevel()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int updatePractice(DictionaryPracticeUpdateDTO update) {
        if (update == null || update.getDictIds() == null || update.getDictIds().isEmpty()
                || update.getItems() == null || update.getItems().isEmpty()) {
            return 0;
        }
        List<DictionaryPracticeUpdateDTO.PracticeItem> items = update.getItems().stream()
                .filter(item -> item != null && item.getWord() != null && !item.getWord().isBlank())
                .limit(MAX_PRACTICE_ITEMS)
                .toList();
        if (items.isEmpty()) {
            return 0;
        }
        Map<String, DictionaryPracticeUpdateDTO.PracticeItem> byWord = items.stream()
                .collect(Collectors.toMap(DictionaryPracticeUpdateDTO.PracticeItem::getWord, item -> item, (left, right) -> {
                    left.setPracticeCount(safeInt(left.getPracticeCount()) + safeInt(right.getPracticeCount()));
                    left.setErrorCount(safeInt(left.getErrorCount()) + safeInt(right.getErrorCount()));
                    return left;
                }, java.util.LinkedHashMap::new));
        List<DictionaryWord> words = dictionaryWordRepository.findByDictIdInAndWordIn(
                update.getDictIds(), new LinkedHashSet<>(byWord.keySet()));
        if (words.isEmpty()) {
            return 0;
        }
        Date now = new Date();
        List<DictionaryWord> updated = new ArrayList<>(words.size());
        for (DictionaryWord word : words) {
            DictionaryPracticeUpdateDTO.PracticeItem item = byWord.get(word.getWord());
            if (item == null) {
                continue;
            }
            int practiceCount = Math.max(0, safeInt(item.getPracticeCount()));
            int errorCount = Math.max(0, safeInt(item.getErrorCount()));
            int total = safeInt(word.getTotalPracticeCount()) + practiceCount;
            int errors = safeInt(word.getErrorPracticeCount()) + errorCount;
            word.setTotalPracticeCount(total);
            word.setErrorPracticeCount(errors);
            word.setUserErrorRate(BigDecimal.valueOf(errors * 100.0 / Math.max(1, total)).setScale(2, RoundingMode.HALF_UP));
            word.setUserContinuousCorrect(errorCount == 0 ? safeInt(word.getUserContinuousCorrect()) + practiceCount : 0);
            word.setProficiencyLevel(resolveProficiency(word));
            word.setDifficultyLevel(resolveDifficulty(word));
            word.setLastPracticeTime(now);
            updated.add(word);
        }
        if (updated.isEmpty()) {
            return 0;
        }
        dictionaryWordRepository.saveAll(updated);
        log.debug("内部接口更新词库练习统计: dictIds={}, 命中词条={}, 更新条数={}",
                update.getDictIds(), byWord.size(), updated.size());
        return updated.size();
    }

    private Integer resolveProficiency(DictionaryWord word) {
        int streak = safeInt(word.getUserContinuousCorrect());
        double errorRate = word.getUserErrorRate() == null ? 0 : word.getUserErrorRate().doubleValue();
        if (streak >= 10 && errorRate <= 5) {
            return 5;
        }
        if (streak >= 6 && errorRate <= 10) {
            return 4;
        }
        if (streak >= 3 && errorRate <= 20) {
            return 3;
        }
        return errorRate >= 40 ? 1 : 2;
    }

    private Integer resolveDifficulty(DictionaryWord word) {
        double errorRate = word.getUserErrorRate() == null ? 0 : word.getUserErrorRate().doubleValue();
        if (errorRate >= 60) {
            return 5;
        }
        if (errorRate >= 40) {
            return 4;
        }
        if (errorRate >= 20) {
            return 3;
        }
        return 2;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
