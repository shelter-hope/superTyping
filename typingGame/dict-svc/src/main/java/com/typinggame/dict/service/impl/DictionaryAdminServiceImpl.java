package com.typinggame.dict.service.impl;

import com.typinggame.dict.dictionary.DictionaryFileStore;
import com.typinggame.dict.dictionary.DictionaryTextParser;
import com.typinggame.dict.repository.CustomDictionaryRepository;
import com.typinggame.dict.repository.SystemDictionaryRepository;
import com.typinggame.dict.repository.SystemDictionaryWordRepository;
import com.typinggame.dict.service.DictionaryAdminService;
import com.typinggame.entity.dictionaries.CustomDictionary;
import com.typinggame.entity.dictionaries.SystemDictionary;
import com.typinggame.entity.dictionaries.SystemDictionaryWord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DictionaryAdminServiceImpl implements DictionaryAdminService {
    private final SystemDictionaryRepository systemDictionaryRepository;
    private final SystemDictionaryWordRepository systemDictionaryWordRepository;
    private final CustomDictionaryRepository customDictionaryRepository;
    private final DictionaryFileStore fileStore;

    public DictionaryAdminServiceImpl(SystemDictionaryRepository systemDictionaryRepository,
                                      SystemDictionaryWordRepository systemDictionaryWordRepository,
                                      CustomDictionaryRepository customDictionaryRepository,
                                      DictionaryFileStore fileStore) {
        this.systemDictionaryRepository = systemDictionaryRepository;
        this.systemDictionaryWordRepository = systemDictionaryWordRepository;
        this.customDictionaryRepository = customDictionaryRepository;
        this.fileStore = fileStore;
    }

    @Override
    @Transactional
    public SystemDictionary createSystemDictionary(Map<String, Object> body) {
        SystemDictionary dictionary = new SystemDictionary();
        dictionary.setName(requiredText(body, "name"));
        dictionary.setDescription(text(body, "description"));
        dictionary.setCategory(text(body, "category"));
        dictionary.setIsActive(bool(body.get("isActive"), true));
        dictionary.setWordCount(0);
        dictionary.setCreateTime(new Date());
        dictionary.setUpdateTime(new Date());
        return systemDictionaryRepository.save(dictionary);
    }

    @Override
    @Transactional
    public SystemDictionary updateSystemDictionary(Integer dictId, Map<String, Object> body) {
        SystemDictionary dictionary = requireDictionary(dictId);
        if (body.containsKey("name") && text(body, "name") != null) dictionary.setName(text(body, "name"));
        if (body.containsKey("description")) dictionary.setDescription(text(body, "description"));
        if (body.containsKey("category")) dictionary.setCategory(text(body, "category"));
        if (body.containsKey("isActive")) dictionary.setIsActive(bool(body.get("isActive"), true));
        dictionary.setUpdateTime(new Date());
        return systemDictionaryRepository.save(dictionary);
    }

    @Override
    @Transactional
    public SystemDictionary setSystemDictionaryActive(Integer dictId, boolean active) {
        SystemDictionary dictionary = requireDictionary(dictId);
        dictionary.setIsActive(active);
        dictionary.setUpdateTime(new Date());
        return systemDictionaryRepository.save(dictionary);
    }

    @Override
    @Transactional
    public void deleteSystemDictionary(Integer dictId) {
        requireDictionary(dictId);
        systemDictionaryWordRepository.deleteByDictId(dictId);
        systemDictionaryRepository.deleteById(dictId);
    }

    @Override
    @Transactional
    public Map<String, Object> importSystemDictionary(Map<String, Object> body) {
        String filename = requiredText(body, "filename");
        String text = requiredText(body, "text");
        List<String> words = DictionaryTextParser.parse(filename, text);
        SystemDictionary dictionary;
        Object dictId = body.get("dictId");
        if (dictId instanceof Number number) {
            dictionary = requireDictionary(number.intValue());
        } else {
            dictionary = new SystemDictionary();
            dictionary.setName(text(body, "name") == null ? filename : text(body, "name"));
            dictionary.setDescription(text(body, "description"));
            dictionary.setCategory(text(body, "category"));
            dictionary.setIsActive(true);
            dictionary.setWordCount(0);
            dictionary.setCreateTime(new Date());
            dictionary = systemDictionaryRepository.save(dictionary);
        }
        DictionaryFileStore.Stored stored = fileStore.save(filename, text.getBytes(StandardCharsets.UTF_8));
        dictionary.setFileName(stored.fileName());
        dictionary.setFileUrl(stored.fileUrl());
        for (String word : words) {
            SystemDictionaryWord row = new SystemDictionaryWord();
            row.setDictId(dictionary.getDictId());
            row.setWord(word);
            row.setDifficultyLevel(1);
            systemDictionaryWordRepository.save(row);
        }
        dictionary.setWordCount(systemDictionaryWordRepository.countByDictId(dictionary.getDictId()));
        dictionary.setUpdateTime(new Date());
        systemDictionaryRepository.save(dictionary);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dictionary", dictionary);
        result.put("imported", words.size());
        result.put("fileUrl", stored.fileUrl());
        return result;
    }

    @Override
    @Transactional
    public SystemDictionaryWord createWord(Map<String, Object> body) {
        Integer dictId = body.get("dictId") instanceof Number number ? number.intValue() : null;
        requireDictionary(dictId);
        SystemDictionaryWord word = new SystemDictionaryWord();
        word.setDictId(dictId);
        word.setWord(requiredText(body, "word"));
        word.setDifficultyLevel(difficulty(body.get("difficultyLevel")));
        SystemDictionaryWord saved = systemDictionaryWordRepository.save(word);
        refreshCount(dictId);
        return saved;
    }

    @Override
    @Transactional
    public SystemDictionaryWord updateWord(Integer wordId, Map<String, Object> body) {
        SystemDictionaryWord word = systemDictionaryWordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("单词不存在"));
        if (body.containsKey("word") && text(body, "word") != null) word.setWord(text(body, "word"));
        if (body.containsKey("difficultyLevel")) word.setDifficultyLevel(difficulty(body.get("difficultyLevel")));
        return systemDictionaryWordRepository.save(word);
    }

    @Override
    @Transactional
    public void deleteWord(Integer wordId) {
        SystemDictionaryWord word = systemDictionaryWordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("单词不存在"));
        systemDictionaryWordRepository.delete(word);
        refreshCount(word.getDictId());
    }

    @Override
    public String exportWords(Integer dictId) {
        requireDictionary(dictId);
        StringBuilder csv = new StringBuilder("word,difficultyLevel\n");
        systemDictionaryWordRepository.findByDictId(dictId, org.springframework.data.domain.PageRequest.of(0, 10000))
                .forEach(word -> csv.append(escape(word.getWord())).append(',').append(word.getDifficultyLevel() == null ? 1 : word.getDifficultyLevel()).append('\n'));
        return csv.toString();
    }

    @Override
    @Transactional
    public CustomDictionary setCustomVisibility(Integer dictId, boolean isPublic) {
        CustomDictionary dictionary = customDictionaryRepository.findById(dictId)
                .orElseThrow(() -> new IllegalArgumentException("自定义词库不存在"));
        dictionary.setIsPublic(isPublic);
        dictionary.setUpdateTime(new Date());
        return customDictionaryRepository.save(dictionary);
    }

    private void refreshCount(Integer dictId) {
        SystemDictionary dictionary = requireDictionary(dictId);
        dictionary.setWordCount(systemDictionaryWordRepository.countByDictId(dictId));
        dictionary.setUpdateTime(new Date());
        systemDictionaryRepository.save(dictionary);
    }

    private SystemDictionary requireDictionary(Integer dictId) {
        if (dictId == null) throw new IllegalArgumentException("词库不存在");
        return systemDictionaryRepository.findById(dictId).orElseThrow(() -> new IllegalArgumentException("词库不存在"));
    }

    private static String requiredText(Map<String, Object> body, String key) {
        String value = text(body, key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " 不能为空");
        return value;
    }

    private static String text(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static boolean bool(Object value, boolean fallback) {
        if (value instanceof Boolean flag) return flag;
        if (value instanceof String text) return Boolean.parseBoolean(text);
        return fallback;
    }

    private static int difficulty(Object value) {
        int level = value instanceof Number number ? number.intValue() : 1;
        return Math.max(1, Math.min(5, level));
    }

    private static String escape(String word) {
        if (word == null) return "";
        if (word.contains(",") || word.contains("\"")) return "\"" + word.replace("\"", "\"\"") + "\"";
        return word;
    }
}
