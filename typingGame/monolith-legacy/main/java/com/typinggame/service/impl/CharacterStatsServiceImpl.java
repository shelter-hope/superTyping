package com.typinggame.service.impl;

import com.typinggame.dto.stats.CharacterStatsBatchUpdateDTO;
import com.typinggame.dto.stats.CharacterStatsUpdateDTO;
import com.typinggame.entity.user.CharacterStats;
import com.typinggame.repository.CharacterStatsRepository;
import com.typinggame.service.CharacterStatsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CharacterStatsServiceImpl implements CharacterStatsService {

    private static final Logger log = LoggerFactory.getLogger(CharacterStatsServiceImpl.class);

    private final CharacterStatsRepository characterStatsRepository;

    @Override
    @Transactional
    public void batchUpdate(Integer userId, CharacterStatsBatchUpdateDTO batchDTO) {
        if (batchDTO == null || batchDTO.getUpdates() == null) {
            return;
        }
        List<CharacterStatsUpdateDTO> updates = batchDTO.getUpdates();
        log.info("CharacterStats batchUpdate: userId={} size={}", userId, updates.size());
        for (CharacterStatsUpdateDTO update : updates) {
            updateSingleChar(userId, update);
        }
    }

    private void updateSingleChar(Integer userId, CharacterStatsUpdateDTO update) {
        String charValue = update.getCharValue();
        if (charValue == null || (charValue = charValue.trim()).isEmpty()) {
            return;
        }

        Optional<CharacterStats> existingStats = characterStatsRepository.findByUserIdAndCharValue(userId, charValue);
        CharacterStats stats;
        boolean isNew = !existingStats.isPresent();

        if (existingStats.isPresent()) {
            stats = existingStats.get();
        } else {
            stats = new CharacterStats();
            stats.setUserId(userId);
            stats.setCharValue(charValue);
            stats.setTotalAttempts(0);
            stats.setCorrectAttempts(0);
            stats.setErrorCount(0);
            stats.setAverageAccuracy(BigDecimal.ZERO);
            stats.setRecentAccuracy(BigDecimal.ZERO);
            String type = update.getCharType();
            if (type == null || type.trim().isEmpty()) {
                type = determineCharType(charValue);
            }
            stats.setCharType(type);
        }

        int total = nullSafeInt(stats.getTotalAttempts()) + 1;
        int correct = nullSafeInt(stats.getCorrectAttempts());
        int errors = nullSafeInt(stats.getErrorCount());
        if (Boolean.TRUE.equals(update.getIsCorrect())) {
            correct += 1;
        } else {
            errors += 1;
        }

        stats.setTotalAttempts(total);
        stats.setCorrectAttempts(correct);
        stats.setErrorCount(errors);
        if (total > 0) {
            BigDecimal accuracy = BigDecimal.valueOf(correct)
                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            stats.setAverageAccuracy(accuracy);
        }
        stats.setUpdateTime(new Date());
        characterStatsRepository.save(stats);
        characterStatsRepository.flush(); // 确保同批内重复字符时，下一次 find 能读到刚插入的行
        if (isNew) {
            log.debug("CharacterStats new char: userId={} char='{}'", userId, charValue);
        }
    }

    private static int nullSafeInt(Integer v) {
        return v == null ? 0 : v;
    }

    private String determineCharType(String charValue) {
        if (charValue.length() != 1) return "unknown";
        char c = charValue.charAt(0);
        if (Character.isLowerCase(c)) return "lower";
        if (Character.isUpperCase(c)) return "upper";
        if (Character.isDigit(c)) return "number";
        if (isChinese(c)) return "chinese";
        return "punct"; // Default to punctuation/special
    }
    
    private boolean isChinese(char c) {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
    }

    @Override
    public List<CharacterStats> getUserStats(Integer userId) {
        return characterStatsRepository.findByUserId(userId);
    }

    @Override
    public List<CharacterStats> getWeakChars(Integer userId, int limit) {
        return characterStatsRepository.findByUserIdOrderByErrorCountDesc(userId, PageRequest.of(0, limit));
    }

    @Override
    public List<CharacterStats> getLowAccuracyChars(Integer userId, int limit) {
        return characterStatsRepository.findByUserIdOrderByAverageAccuracyAsc(userId, PageRequest.of(0, limit));
    }
}
