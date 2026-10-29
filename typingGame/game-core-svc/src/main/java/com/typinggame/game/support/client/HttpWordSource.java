package com.typinggame.game.support.client;

import com.typinggame.dto.dictionary.DictionaryIdsQueryDTO;
import com.typinggame.game.service.word.WordSource;
import com.typinggame.dto.dictionary.DictionaryWordItemDTO;
import com.typinggame.dto.dictionary.DictionaryWordsQueryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 词库读端口的外部实现：全部走 dict-svc 的内部接口，game-core 不再直连词库表。
 * 词库服务不可用时按「空词库」降级（记录告警），避免把整个对局请求打成 500。
 */
@Component
public class HttpWordSource implements WordSource {
    private static final Logger log = LoggerFactory.getLogger(HttpWordSource.class);
    private static final ParameterizedTypeReference<List<Integer>> ID_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<DictionaryWordItemDTO>> WORD_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HttpWordSource(RestTemplate gameRestTemplate,
                          @Value("${game.dict.base-url:http://dict-svc/dict-svc/api/v1/internal/dictionaries}") String baseUrl) {
        this.restTemplate = gameRestTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public List<Integer> allowedCustomDictIds(Collection<Integer> dictIds, Integer userId) {
        return postIds("CUSTOM", dictIds, userId);
    }

    @Override
    public List<Integer> activeSystemDictIds(Collection<Integer> dictIds) {
        return postIds("SYSTEM", dictIds, null);
    }

    @Override
    public List<WordEntry> customWords(Collection<Integer> dictIds) {
        return postWords("CUSTOM", dictIds);
    }

    @Override
    public List<WordEntry> systemWords(Collection<Integer> dictIds) {
        return postWords("SYSTEM", dictIds);
    }

    private List<Integer> postIds(String source, Collection<Integer> dictIds, Integer userId) {
        if (dictIds == null || dictIds.isEmpty()) {
            return List.of();
        }
        try {
            DictionaryIdsQueryDTO query = new DictionaryIdsQueryDTO();
            query.setSource(source);
            query.setDictIds(new ArrayList<>(dictIds));
            query.setUserId(userId);
            ResponseEntity<List<Integer>> response = restTemplate.exchange(
                    baseUrl + "/ids", HttpMethod.POST, new HttpEntity<>(query), ID_LIST);
            return Objects.requireNonNullElse(response.getBody(), List.of());
        } catch (Exception exception) {
            log.warn("dict-svc 词库 ID 查询失败(source={}), 按空词库降级: {}", source, exception.getMessage());
            return List.of();
        }
    }

    private List<WordEntry> postWords(String source, Collection<Integer> dictIds) {
        if (dictIds == null || dictIds.isEmpty()) {
            return List.of();
        }
        try {
            DictionaryWordsQueryDTO query = new DictionaryWordsQueryDTO();
            query.setSource(source);
            query.setDictIds(new ArrayList<>(dictIds));
            ResponseEntity<List<DictionaryWordItemDTO>> response = restTemplate.exchange(
                    baseUrl + "/words", HttpMethod.POST, new HttpEntity<>(query), WORD_LIST);
            List<DictionaryWordItemDTO> items = response.getBody();
            if (items == null || items.isEmpty()) {
                return List.of();
            }
            return items.stream()
                    .filter(item -> item != null && item.getWord() != null && !item.getWord().isBlank())
                    .map(item -> new WordEntry(item.getWord(), item.getDifficultyLevel()))
                    .collect(Collectors.toList());
        } catch (Exception exception) {
            log.warn("dict-svc 取词失败(source={}), 按空词库降级: {}", source, exception.getMessage());
            return List.of();
        }
    }
}
