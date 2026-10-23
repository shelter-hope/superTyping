package com.typinggame.chat.service.impl;

import com.typinggame.chat.dictionary.DictionaryFileStore;
import com.typinggame.chat.dictionary.DictionaryTextParser;
import com.typinggame.chat.repository.CustomDictionaryRepository;
import com.typinggame.chat.repository.DictionaryWordRepository;
import com.typinggame.chat.repository.SystemDictionaryRepository;
import com.typinggame.chat.service.DictionaryService;
import com.typinggame.dto.dictionary.DictionaryCreateDTO;
import com.typinggame.entity.dictionaries.CustomDictionary;
import com.typinggame.entity.dictionaries.DictionaryWord;
import com.typinggame.entity.dictionaries.SystemDictionary;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DictionaryServiceImpl implements DictionaryService {

    private final CustomDictionaryRepository customDictionaryRepository;
    private final DictionaryWordRepository dictionaryWordRepository;
    private final SystemDictionaryRepository systemDictionaryRepository;
    private final DictionaryFileStore dictionaryFileStore;

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
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 txt 或 csv 词库");
        }
        String original = file.getOriginalFilename() == null ? "words.txt" : file.getOriginalFilename();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception exception) {
            throw new IllegalArgumentException("读取词库文件失败");
        }
        List<String> words = DictionaryTextParser.parse(original, new String(bytes, StandardCharsets.UTF_8));
        DictionaryFileStore.Stored stored = dictionaryFileStore.save(original, bytes);

        CustomDictionary dict = new CustomDictionary();
        dict.setUserId(userId);
        dict.setName(dto.getName());
        dict.setDescription(dto.getDescription());
        dict.setIsPublic(dto.getIsPublic());
        dict.setFileName(stored.fileName());
        dict.setFileUrl(stored.fileUrl());
        dict.setWordCount(0);
        dict.setCreateTime(new Date());
        dict.setUpdateTime(new Date());
        dict = customDictionaryRepository.save(dict);
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