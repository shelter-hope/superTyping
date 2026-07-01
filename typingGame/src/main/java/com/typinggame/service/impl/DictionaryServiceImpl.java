package com.typinggame.service.impl;

import com.typinggame.dto.dictionary.DictionaryCreateDTO;
import com.typinggame.entity.dictionaries.CustomDictionary;
import com.typinggame.entity.dictionaries.DictionaryWord;
import com.typinggame.entity.dictionaries.SystemDictionary;
import com.typinggame.repository.CustomDictionaryRepository;
import com.typinggame.repository.DictionaryWordRepository;
import com.typinggame.repository.SystemDictionaryRepository;
import com.typinggame.service.DictionaryService;
import com.typinggame.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DictionaryServiceImpl implements DictionaryService {

    private static final int MAX_WORD_LENGTH = 255;
    private static final Pattern INVALID_CHAR = Pattern.compile("[\\x00-\\x1F\\x7F]");

    private final CustomDictionaryRepository customDictionaryRepository;
    private final DictionaryWordRepository dictionaryWordRepository;
    private final SystemDictionaryRepository systemDictionaryRepository;
    private final FileStorageUtil fileStorageUtil;

    @Override
    public List<SystemDictionary> getSystemDictionaries() {
        return systemDictionaryRepository.findByIsActiveTrue();
    }

    @Override
    public List<CustomDictionary> getUserDictionaries(Integer userId) {
        return customDictionaryRepository.findByUserId(userId);
    }

    @Override
    public CustomDictionary createCustomDictionary(Integer userId, DictionaryCreateDTO dto, MultipartFile file) {
        String fileName = fileStorageUtil.storeFile(file);

        CustomDictionary dict = new CustomDictionary();
        dict.setUserId(userId);
        dict.setName(dto.getName());
        dict.setDescription(dto.getDescription());
        dict.setIsPublic(dto.getIsPublic());
        dict.setFileName(fileName);
        dict.setFileUrl("/uploads/" + fileName);
        dict.setWordCount(0);
        dict.setCreateTime(new Date());
        dict.setUpdateTime(new Date());
        dict = customDictionaryRepository.save(dict);

        List<String> words = parseWordsFromFile(file);
        for (String w : words) {
            DictionaryWord dw = new DictionaryWord();
            dw.setDictId(dict.getDictId());
            dw.setWord(w);
            dictionaryWordRepository.save(dw);
        }
        dict.setWordCount(words.size());
        dict.setUpdateTime(new Date());
        return customDictionaryRepository.save(dict);
    }

    /**
     * 从上传文件中解析词条：按行、空白分割，过滤无效字符与空串，去重，长度限制 [1, MAX_WORD_LENGTH]
     */
    List<String> parseWordsFromFile(MultipartFile file) {
        Set<String> seen = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("[\\s\\p{Z}\\p{C}]+");
                for (String part : parts) {
                    String w = INVALID_CHAR.matcher(part.trim()).replaceAll("").trim();
                    if (w.isEmpty() || w.length() > MAX_WORD_LENGTH) continue;
                    seen.add(w);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析词库文件失败", e);
        }
        return new ArrayList<>(seen);
    }

    @Override
    @Transactional
    public boolean deleteCustomDictionary(Integer userId, Integer dictId) {
        CustomDictionary dict = customDictionaryRepository.findById(dictId).orElse(null);
        if (dict != null && dict.getUserId().equals(userId)) {
            dictionaryWordRepository.deleteByDictId(dictId);
            customDictionaryRepository.delete(dict);
            return true;
        }
        return false;
    }
}
