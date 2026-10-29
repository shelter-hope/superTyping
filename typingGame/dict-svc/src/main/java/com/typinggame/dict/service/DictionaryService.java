package com.typinggame.dict.service;

import com.typinggame.dto.dictionary.DictionaryCreateDTO;
import com.typinggame.dict.entity.CustomDictionary;
import com.typinggame.dict.entity.SystemDictionary;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DictionaryService {
    List<SystemDictionary> getSystemDictionaries();
    List<CustomDictionary> getUserDictionaries(Integer userId);
    CustomDictionary createCustomDictionary(Integer userId, DictionaryCreateDTO dto, MultipartFile file);
    boolean deleteCustomDictionary(Integer userId, Integer dictId);
}