package com.typinggame.game.service.impl;

import com.typinggame.dto.stats.KeyConfusionPairDTO;
import com.typinggame.dto.stats.KeyConfusionUpdateDTO;
import com.typinggame.entity.user.KeyConfusion;
import com.typinggame.game.repository.KeyConfusionRepository;
import com.typinggame.game.service.KeyConfusionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按键混淆档案实现：上报即累加，读取按次数降序。
 */
@Service
@RequiredArgsConstructor
public class KeyConfusionServiceImpl implements KeyConfusionService {

    /** 单次上报最多处理多少条（前端已聚合，这里是防御性上限） */
    private static final int MAX_UPDATES_PER_REPORT = 2000;
    /** 单条混淆一次最多累加多少，避免脏数据把权重拉爆 */
    private static final int MAX_COUNT_PER_ENTRY = 1000;
    /** 个人页回看一次最多返回多少对，防止接口被用来翻全表 */
    private static final int MAX_TOP_CONFUSIONS = 50;

    private final KeyConfusionRepository keyConfusionRepository;

    @Override
    @Transactional
    public void batchUpdate(Integer userId, List<KeyConfusionUpdateDTO> updates) {
        if (userId == null || updates == null || updates.isEmpty()) {
            return;
        }
        Map<String, Integer> aggregated = new LinkedHashMap<>();
        Map<String, String> expectedByKey = new LinkedHashMap<>();
        Map<String, String> typedByKey = new LinkedHashMap<>();
        int processed = 0;
        for (KeyConfusionUpdateDTO update : updates) {
            if (update == null || processed >= MAX_UPDATES_PER_REPORT) {
                continue;
            }
            String expectedChar = normalize(update.getExpectedChar());
            String typedChar = normalize(update.getTypedChar());
            if (expectedChar == null || typedChar == null || expectedChar.equals(typedChar)) {
                continue;
            }
            int count = update.getCount() == null ? 1 : update.getCount();
            if (count <= 0) {
                continue;
            }
            count = Math.min(count, MAX_COUNT_PER_ENTRY);
            String key = expectedChar + '\u0000' + typedChar;
            aggregated.merge(key, count, Integer::sum);
            expectedByKey.put(key, expectedChar);
            typedByKey.put(key, typedChar);
            processed++;
        }
        for (Map.Entry<String, Integer> entry : aggregated.entrySet()) {
            String key = entry.getKey();
            String expectedChar = expectedByKey.get(key);
            String typedChar = typedByKey.get(key);
            int delta = Math.min(entry.getValue(), MAX_COUNT_PER_ENTRY);
            KeyConfusion row = keyConfusionRepository
                    .findByUserIdAndExpectedCharAndTypedChar(userId, expectedChar, typedChar)
                    .orElseGet(() -> create(userId, expectedChar, typedChar));
            row.setConfusionCount(nullSafe(row.getConfusionCount()) + delta);
            row.setUpdateTime(new Date());
            keyConfusionRepository.save(row);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getConfusionMap(Integer userId, int maxExpectedChars, int maxTypedPerExpected) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (userId == null || maxExpectedChars <= 0 || maxTypedPerExpected <= 0) {
            return result;
        }
        int limit = maxExpectedChars * maxTypedPerExpected;
        List<KeyConfusion> rows = keyConfusionRepository.findByUserIdOrderByConfusionCountDesc(userId, PageRequest.of(0, limit));
        for (KeyConfusion row : rows) {
            if (row == null) {
                continue;
            }
            String expectedChar = normalize(row.getExpectedChar());
            String typedChar = normalize(row.getTypedChar());
            if (expectedChar == null || typedChar == null) {
                continue;
            }
            List<String> typedChars = result.get(expectedChar);
            if (typedChars == null) {
                if (result.size() >= maxExpectedChars) {
                    continue;
                }
                typedChars = new ArrayList<>();
                result.put(expectedChar, typedChars);
            }
            if (typedChars.size() < maxTypedPerExpected && !typedChars.contains(typedChar)) {
                typedChars.add(typedChar);
            }
        }
        return result;
    }

    @Override
    public long countByUserId(Integer userId) {
        return userId == null ? 0L : keyConfusionRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KeyConfusionPairDTO> getTopConfusions(Integer userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), MAX_TOP_CONFUSIONS);
        return keyConfusionRepository
                .findByUserIdOrderByConfusionCountDesc(userId, PageRequest.of(0, safeLimit))
                .stream()
                .map(row -> new KeyConfusionPairDTO(
                        row.getExpectedChar(),
                        row.getTypedChar(),
                        nullSafe(row.getConfusionCount())))
                .toList();
    }

    private KeyConfusion create(Integer userId, String expectedChar, String typedChar) {
        KeyConfusion row = new KeyConfusion();
        row.setUserId(userId);
        row.setExpectedChar(expectedChar);
        row.setTypedChar(typedChar);
        row.setConfusionCount(0);
        row.setUpdateTime(new Date());
        return row;
    }

    /** 只接受单个可见字符；空白、多字符、控制字符一律丢弃 */
    private String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.strip();
        if (value.length() != 1 || Character.isISOControl(value.charAt(0))) {
            return null;
        }
        return value;
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}
