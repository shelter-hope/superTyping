package com.typinggame.dict.service;

import com.typinggame.entity.dictionaries.CustomDictionary;
import com.typinggame.entity.dictionaries.SystemDictionary;
import com.typinggame.entity.dictionaries.SystemDictionaryWord;

import java.util.Map;

public interface DictionaryAdminService {
    SystemDictionary createSystemDictionary(Map<String, Object> body);

    SystemDictionary updateSystemDictionary(Integer dictId, Map<String, Object> body);

    SystemDictionary setSystemDictionaryActive(Integer dictId, boolean active);

    void deleteSystemDictionary(Integer dictId);

    Map<String, Object> importSystemDictionary(Map<String, Object> body);

    SystemDictionaryWord createWord(Map<String, Object> body);

    SystemDictionaryWord updateWord(Integer wordId, Map<String, Object> body);

    void deleteWord(Integer wordId);

    String exportWords(Integer dictId);

    CustomDictionary setCustomVisibility(Integer dictId, boolean isPublic);
}
