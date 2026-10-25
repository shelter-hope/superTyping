package com.typinggame.dict.service;

import java.util.Map;

public interface InternalAdminService {
    Map<String, Object> getSystemDictionaries(int page, int pageSize, String name, String category, Boolean isActive);

    Map<String, Object> getCustomDictionaries(int page, int pageSize, Integer userId, String name, Boolean isPublic);

    Map<String, Object> getSystemDictionaryWords(Integer dictId, int page, int pageSize);

    Map<String, Object> getCustomDictionaryWords(Integer dictId, int page, int pageSize);

    Map<String, Object> getOverview();
}
