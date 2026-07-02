package com.typinggame.game.service.impl;

import com.typinggame.dto.stats.CharacterStatsBatchUpdateDTO;
import com.typinggame.dto.stats.CharacterStatsUpdateDTO;
import com.typinggame.entity.user.CharacterStats;
import com.typinggame.game.repository.CharacterStatsRepository;
import com.typinggame.game.service.CharacterStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CharacterStatsServiceImpl implements CharacterStatsService {

    private final CharacterStatsRepository characterStatsRepository;

    @Override
    @Transactional
    public void batchUpdate(Integer userId, CharacterStatsBatchUpdateDTO batchDTO) {
        if (batchDTO == null || batchDTO.getUpdates() == null) {
            return;
        }
        for (CharacterStatsUpdateDTO update : batchDTO.getUpdates()) {
            updateSingleChar(userId, update);
        }
    }

    private void updateSingleChar(Integer userId, CharacterStatsUpdateDTO update) {
        if (update == null) {
            return;
        }
        String rawCharValue = update.getCharValue();
        if (rawCharValue == null || rawCharValue.trim().isEmpty()) {
            return;
        }
        String charValue = rawCharValue.trim();

        CharacterStats stats = characterStatsRepository.findByUserIdAndCharValue(userId, charValue)
                .orElseGet(() -> createStats(userId, charValue, update.getCharType()));

        int total = nullSafeInt(stats.getTotalAttempts()) + 1;
        int correct = nullSafeInt(stats.getCorrectAttempts());
        int errors = nullSafeInt(stats.getErrorCount());
        if (Boolean.TRUE.equals(update.getIsCorrect())) {
            correct++;
        } else {
            errors++;
        }

        stats.setTotalAttempts(total);
        stats.setCorrectAttempts(correct);
        stats.setErrorCount(errors);
        stats.setAverageAccuracy(BigDecimal.valueOf(correct)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
        stats.setRecentAccuracy(stats.getAverageAccuracy());
        stats.setUpdateTime(new Date());
        characterStatsRepository.saveAndFlush(stats);
    }

    private CharacterStats createStats(Integer userId, String charValue, String charType) {
        CharacterStats stats = new CharacterStats();
        stats.setUserId(userId);
        stats.setCharValue(charValue);
        stats.setTotalAttempts(0);
        stats.setCorrectAttempts(0);
        stats.setErrorCount(0);
        stats.setAverageAccuracy(BigDecimal.ZERO);
        stats.setRecentAccuracy(BigDecimal.ZERO);
        stats.setCharType((charType == null || charType.isBlank()) ? determineCharType(charValue) : charType);
        stats.setUpdateTime(new Date());
        return stats;
    }

    private static int nullSafeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String determineCharType(String charValue) {
        if (charValue.length() != 1) return "unknown";
        char c = charValue.charAt(0);
        if (Character.isLowerCase(c)) return "lower";
        if (Character.isUpperCase(c)) return "upper";
        if (Character.isDigit(c)) return "number";
        if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) return "chinese";
        return "punct";
    }

    @Override
    public List<CharacterStats> getUserStats(Integer userId) {
        return characterStatsRepository.findByUserId(userId);
    }

    @Override
    public List<CharacterStats> getWeakChars(Integer userId, int limit) {
        return characterStatsRepository.findByUserIdOrderByErrorCountDesc(userId, PageRequest.of(0, Math.max(1, limit)));
    }

    @Override
    public List<CharacterStats> getLowAccuracyChars(Integer userId, int limit) {
        return characterStatsRepository.findByUserIdOrderByAverageAccuracyAsc(userId, PageRequest.of(0, Math.max(1, limit)));
    }
}
