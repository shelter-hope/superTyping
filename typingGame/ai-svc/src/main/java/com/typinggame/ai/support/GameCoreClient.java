package com.typinggame.ai.support;

import com.typinggame.dto.game.GameRecordSummaryDTO;
import com.typinggame.dto.stats.WeakCharDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * game-core 只读数据客户端。
 *
 * <p>对局与字符统计都属于 game-core 的数据边界，ai-svc 不再直连这些表；
 * 远程失败时按空结果降级并告警，保证 AI 建议接口不因下游抖动而失败。</p>
 */
@Component
public class GameCoreClient {

    private static final Logger log = LoggerFactory.getLogger(GameCoreClient.class);
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public GameCoreClient(@Qualifier("restTemplate") RestTemplate restTemplate,
                          @Value("${ai.game-core.base-url:http://game-core-svc/game-core-svc}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<GameRecordSummaryDTO> recentRecords(Integer userId, int limit) {
        String url = baseUrl + "/api/v1/internal/player-data/game-records?userId={userId}&limit={limit}";
        return getList(url, new ParameterizedTypeReference<List<GameRecordSummaryDTO>>() {
        }, userId, limit);
    }

    public List<GameRecordSummaryDTO> recordsBetween(Integer userId, Date startDate, Date endDate) {
        String url = baseUrl + "/api/v1/internal/player-data/game-records?userId={userId}&startDate={startDate}&endDate={endDate}";
        return getList(url, new ParameterizedTypeReference<List<GameRecordSummaryDTO>>() {
        }, userId, toIso(startDate), toIso(endDate));
    }

    public List<WeakCharDTO> weakChars(Integer userId, int limit) {
        String url = baseUrl + "/api/v1/internal/player-data/weak-chars?userId={userId}&limit={limit}";
        return getList(url, new ParameterizedTypeReference<List<WeakCharDTO>>() {
        }, userId, limit);
    }

    private <T> List<T> getList(String url, ParameterizedTypeReference<List<T>> type, Object... uriVariables) {
        try {
            ResponseEntity<List<T>> response = restTemplate.exchange(url, HttpMethod.GET, null, type, uriVariables);
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (RestClientException e) {
            log.warn("game-core 内部接口调用失败，按空结果降级：{}", e.getMessage());
            return List.of();
        }
    }

    private String toIso(Date date) {
        if (date == null) {
            return "";
        }
        return ISO_DATE_TIME.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }
}
