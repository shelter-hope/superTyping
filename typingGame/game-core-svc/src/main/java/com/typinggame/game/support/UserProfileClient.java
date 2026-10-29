package com.typinggame.game.support;

import com.typinggame.dto.user.DailyStatsUpdateDTO;
import com.typinggame.dto.user.UserGameSummaryDTO;
import com.typinggame.dto.user.UserProfileDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 用户数据的集群内访问入口：game-core 不再直连 user_info 表。
 * 读取失败按「用户不存在」降级并告警；回写失败只告警，不影响已入库的对局结果。
 */
@Component
public class UserProfileClient {
    private static final Logger log = LoggerFactory.getLogger(UserProfileClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UserProfileClient(RestTemplate gameRestTemplate,
                             @Value("${game.user.base-url:http://user-svc/user-svc/api/v1/internal/users}") String baseUrl) {
        this.restTemplate = gameRestTemplate;
        this.baseUrl = baseUrl;
    }

    public UserProfileDTO fetchProfile(Integer userId) {
        if (userId == null) {
            return null;
        }
        try {
            return restTemplate.getForObject(baseUrl + "/" + userId, UserProfileDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            log.warn("fetchProfile({}) failed: {}", userId, e.getMessage());
            return null;
        }
    }

    public boolean exists(Integer userId) {
        return fetchProfile(userId) != null;
    }

    /** 回写一局汇总（最佳成绩判定与总场数累加在 user-svc 完成）。 */
    public void applyGameSummary(Integer userId, UserGameSummaryDTO summary) {
        if (userId == null || summary == null) {
            return;
        }
        try {
            restTemplate.postForEntity(baseUrl + "/" + userId + "/game-summary", summary, UserProfileDTO.class);
        } catch (Exception e) {
            log.warn("applyGameSummary({}) failed: {}", userId, e.getMessage());
        }
    }

    /** 累加一局的对局统计（加权均值、峰值与场次累加都在 user-svc 完成）。 */
    public void upsertDailyStats(Integer userId, DailyStatsUpdateDTO update) {
        if (userId == null || update == null) {
            return;
        }
        try {
            restTemplate.postForEntity(baseUrl + "/" + userId + "/daily-stats", update, Map.class);
        } catch (Exception e) {
            log.warn("upsertDailyStats({}) failed: {}", userId, e.getMessage());
        }
    }
}
