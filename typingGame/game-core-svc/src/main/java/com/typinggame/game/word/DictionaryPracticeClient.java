package com.typinggame.game.word;

import com.typinggame.dto.dictionary.DictionaryPracticeUpdateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 结算后把自定义词库词的练习/错误次数回写 dict-svc。
 * 写失败只记录告警：结算主流程（分数、难度、成长记录）不能被词库服务拖垮。
 */
@Component
public class DictionaryPracticeClient {
    private static final Logger log = LoggerFactory.getLogger(DictionaryPracticeClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public DictionaryPracticeClient(RestTemplate gameRestTemplate,
                                    @Value("${game.dict.base-url:http://dict-svc/dict-svc/api/v1/internal/dictionaries}") String baseUrl) {
        this.restTemplate = gameRestTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public int recordPractice(List<Integer> dictIds, List<DictionaryPracticeUpdateDTO.PracticeItem> items) {
        if (dictIds == null || dictIds.isEmpty() || items == null || items.isEmpty()) {
            return 0;
        }
        try {
            DictionaryPracticeUpdateDTO update = new DictionaryPracticeUpdateDTO();
            update.setDictIds(dictIds);
            update.setItems(items);
            Map<?, ?> body = restTemplate.postForObject(baseUrl + "/practice", update, Map.class);
            Object updated = body == null ? null : body.get("updated");
            return updated instanceof Number number ? number.intValue() : 0;
        } catch (Exception exception) {
            log.warn("词库练习统计回写失败(dictIds={}, 词条={}), 结算继续: {}",
                    dictIds, items.size(), exception.getMessage());
            return 0;
        }
    }
}
