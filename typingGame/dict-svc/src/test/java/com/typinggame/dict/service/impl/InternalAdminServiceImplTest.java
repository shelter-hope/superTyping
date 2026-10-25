package com.typinggame.dict.service.impl;

import com.typinggame.dict.repository.CustomDictionaryRepository;
import com.typinggame.dict.repository.DictionaryWordRepository;
import com.typinggame.dict.repository.SystemDictionaryRepository;
import com.typinggame.dict.repository.SystemDictionaryWordRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalAdminServiceImplTest {

    @Test
    void overviewCountsSystemAndCustomDictionaries() {
        SystemDictionaryRepository system = mock(SystemDictionaryRepository.class);
        CustomDictionaryRepository custom = mock(CustomDictionaryRepository.class);
        when(system.count()).thenReturn(4L);
        when(custom.count()).thenReturn(7L);

        InternalAdminServiceImpl service = new InternalAdminServiceImpl(
                system, custom, mock(SystemDictionaryWordRepository.class), mock(DictionaryWordRepository.class));

        Map<String, Object> overview = service.getOverview();

        assertEquals(4L, overview.get("systemDictionaryCount"));
        assertEquals(7L, overview.get("customDictionaryCount"));
    }
}
