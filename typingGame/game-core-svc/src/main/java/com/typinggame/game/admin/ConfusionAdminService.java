package com.typinggame.game.admin;

import com.typinggame.entity.user.KeyConfusion;
import com.typinggame.game.repository.KeyConfusionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端「按键混淆」查询：按用户看它把哪些键打成了什么。
 *
 * <p>数据来自 game-core-svc 的逐键埋点（key_confusion 表），每行是「期望字符 → 实际打出的字符」
 * 的累计次数。这里同时给出扁平明细与按期望字符聚合的视图，方便管理员判断某个键的混淆方向。</p>
 */
@Service
public class ConfusionAdminService {
    static final int MAX_LIMIT = 100;

    private final KeyConfusionRepository keyConfusionRepository;

    public ConfusionAdminService(KeyConfusionRepository keyConfusionRepository) {
        this.keyConfusionRepository = keyConfusionRepository;
    }

    /**
     * @param userId 目标用户
     * @param limit  明细条数上限（1..100），按累计次数降序
     * @return userId、limit、total、items（明细）、byExpected（按期望字符聚合）
     */
    public Map<String, Object> getKeyConfusion(Integer userId, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        long total = userId == null ? 0L : keyConfusionRepository.countByUserId(userId);
        List<KeyConfusion> rows = userId == null
                ? List.of()
                : keyConfusionRepository.findByUserIdOrderByConfusionCountDesc(userId, PageRequest.of(0, cappedLimit));

        List<Map<String, Object>> items = new ArrayList<>(rows.size());
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (KeyConfusion row : rows) {
            int count = row.getConfusionCount() == null ? 0 : row.getConfusionCount();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("expectedChar", row.getExpectedChar());
            item.put("typedChar", row.getTypedChar());
            item.put("confusionCount", count);
            items.add(item);
            grouped.computeIfAbsent(row.getExpectedChar(), key -> new ArrayList<>()).add(item);
        }

        List<Map<String, Object>> byExpected = new ArrayList<>(grouped.size());
        grouped.forEach((expectedChar, entries) -> {
            int sum = entries.stream().mapToInt(entry -> ((Integer) entry.get("confusionCount"))).sum();
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("expectedChar", expectedChar);
            group.put("total", sum);
            group.put("items", entries);
            byExpected.add(group);
        });
        byExpected.sort((left, right) -> Integer.compare((Integer) right.get("total"), (Integer) left.get("total")));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("limit", cappedLimit);
        data.put("total", total);
        data.put("items", items);
        data.put("byExpected", byExpected);
        return data;
    }
}
