package com.typinggame.dict.service.impl;

import com.typinggame.common.admin.AdminFilter;
import com.typinggame.common.admin.AdminPageSupport;
import com.typinggame.dict.repository.CustomDictionaryRepository;
import com.typinggame.dict.repository.DictionaryWordRepository;
import com.typinggame.dict.repository.SystemDictionaryRepository;
import com.typinggame.dict.repository.SystemDictionaryWordRepository;
import com.typinggame.dict.service.InternalAdminService;
import com.typinggame.dict.entity.CustomDictionary;
import com.typinggame.dict.entity.SystemDictionary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InternalAdminServiceImpl implements InternalAdminService {
    private final SystemDictionaryRepository systemDictionaryRepository;
    private final CustomDictionaryRepository customDictionaryRepository;
    private final SystemDictionaryWordRepository systemDictionaryWordRepository;
    private final DictionaryWordRepository dictionaryWordRepository;

    public InternalAdminServiceImpl(SystemDictionaryRepository systemDictionaryRepository,
                                    CustomDictionaryRepository customDictionaryRepository,
                                    SystemDictionaryWordRepository systemDictionaryWordRepository,
                                    DictionaryWordRepository dictionaryWordRepository) {
        this.systemDictionaryRepository = systemDictionaryRepository;
        this.customDictionaryRepository = customDictionaryRepository;
        this.systemDictionaryWordRepository = systemDictionaryWordRepository;
        this.dictionaryWordRepository = dictionaryWordRepository;
    }

    @Override
    public Map<String, Object> getSystemDictionaries(int page, int pageSize, String name, String category, Boolean isActive) {
        AdminFilter<SystemDictionary> filter = AdminFilter.of(SystemDictionary.class)
                .like("name", name).like("category", category).eq("isActive", isActive);
        return AdminPageSupport.pageMap("items", systemDictionaryRepository.findAll(filter, pageable(page, pageSize, "dictId")));
    }

    @Override
    public Map<String, Object> getCustomDictionaries(int page, int pageSize, Integer userId, String name, Boolean isPublic) {
        AdminFilter<CustomDictionary> filter = AdminFilter.of(CustomDictionary.class)
                .eq("userId", userId).like("name", name).eq("isPublic", isPublic);
        return AdminPageSupport.pageMap("items", customDictionaryRepository.findAll(filter, pageable(page, pageSize, "dictId")));
    }

    @Override
    public Map<String, Object> getSystemDictionaryWords(Integer dictId, int page, int pageSize) {
        return AdminPageSupport.pageMap("items", systemDictionaryWordRepository.findByDictId(dictId, pageable(page, pageSize, null)));
    }

    @Override
    public Map<String, Object> getCustomDictionaryWords(Integer dictId, int page, int pageSize) {
        return AdminPageSupport.pageMap("items", dictionaryWordRepository.findByDictId(dictId, pageable(page, pageSize, null)));
    }

    @Override
    public Map<String, Object> getOverview() {
        return AdminPageSupport.map(
                "systemDictionaryCount", systemDictionaryRepository.count(),
                "customDictionaryCount", customDictionaryRepository.count());
    }

    private PageRequest pageable(int page, int pageSize, String sortDescField) {
        int p = Math.max(0, page - 1);
        int size = Math.min(200, Math.max(1, pageSize));
        return sortDescField == null
                ? PageRequest.of(p, size)
                : PageRequest.of(p, size, Sort.by(Sort.Direction.DESC, sortDescField));
    }
}
