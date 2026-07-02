package com.typinggame.ai.service.impl;

import com.typinggame.ai.repository.CharacterStatsRepository;
import com.typinggame.ai.repository.GameRecordRepository;
import com.typinggame.ai.service.AiService;
import com.typinggame.dto.ai.AiTrainingSummaryDTO;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.user.CharacterStats;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiServiceImpl implements AiService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    private final CharacterStatsRepository characterStatsRepository;
    private final GameRecordRepository gameRecordRepository;

    public AiServiceImpl(CharacterStatsRepository characterStatsRepository, GameRecordRepository gameRecordRepository) {
        this.characterStatsRepository = characterStatsRepository;
        this.gameRecordRepository = gameRecordRepository;
    }

    @Override
    public AiTrainingSummaryDTO getTrainingSummary(Integer userId) {
        List<CharacterStats> weakChars = characterStatsRepository
                .findByUserIdOrderByAverageAccuracyAsc(userId, PageRequest.of(0, 10));
        List<GameRecord> recentRecords = gameRecordRepository.findByUserId(userId, PageRequest.of(0, 20));
        List<String> focusChars = weakChars.stream()
                .map(CharacterStats::getCharValue)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
        double recentAccuracy = recentRecords.stream()
                .mapToDouble(r -> r.getAvgAccuracy() == null ? 0 : r.getAvgAccuracy().doubleValue())
                .average()
                .orElse(0);
        double recentSpeed = recentRecords.stream()
                .mapToDouble(r -> r.getAvgSpeed() == null ? 0 : r.getAvgSpeed().doubleValue())
                .average()
                .orElse(0);
        String errorSummary = buildErrorComparisonSummary(userId);
        String context = buildContextForPrompt(focusChars, recentAccuracy, recentSpeed, errorSummary);
        return AiTrainingSummaryDTO.builder()
                .contextForPrompt(context)
                .focusChars(focusChars)
                .suggestedMode("endless")
                .recentAccuracy(recentAccuracy)
                .recentSpeed(recentSpeed)
                .errorComparisonSummary(errorSummary)
                .build();
    }

    private String buildErrorComparisonSummary(Integer userId) {
        LocalDate now = LocalDate.now(DEFAULT_ZONE);
        LocalDate thisWeekStart = now.minusDays(6);
        LocalDate lastWeekStart = now.minusDays(13);
        int thisWeek = countErrors(gameRecordRepository.findByUserIdAndCreateTimeBetween(
                userId, atStartOfDay(thisWeekStart), atEndOfDay(now)));
        int lastWeek = countErrors(gameRecordRepository.findByUserIdAndCreateTimeBetween(
                userId, atStartOfDay(lastWeekStart), atStartOfDay(thisWeekStart)));
        int change = thisWeek - lastWeek;
        StringBuilder sb = new StringBuilder();
        sb.append("本周错误数：").append(thisWeek).append("，上周：").append(lastWeek);
        sb.append("；");
        if (change > 0) sb.append("较上周增加 ").append(change).append(" 次");
        else if (change < 0) sb.append("较上周减少 ").append(-change).append(" 次");
        else sb.append("与上周持平");
        sb.append("。");
        return sb.toString();
    }

    private String buildContextForPrompt(List<String> focusChars, double recentAccuracy, double recentSpeed, String errorSummary) {
        StringBuilder sb = new StringBuilder();
        sb.append("【用户打字训练数据摘要】\n");
        sb.append("近期表现：平均准确率 ").append(String.format("%.1f", recentAccuracy)).append("%")
                .append("，平均速度 ").append(String.format("%.1f", recentSpeed)).append("。\n");
        if (errorSummary != null && !errorSummary.isBlank()) {
            sb.append("错误趋势：").append(errorSummary).append("\n");
        }
        if (focusChars != null && !focusChars.isEmpty()) {
            String chars = focusChars.stream().limit(10).collect(Collectors.joining("、"));
            sb.append("建议重点练习的字符：").append(chars).append("。");
        } else {
            sb.append("暂无足够字符统计数据，建议先完成几局无尽练习以积累样本。");
        }
        return sb.toString().trim();
    }

    private int countErrors(List<GameRecord> records) {
        if (records == null) return 0;
        return Math.max(0, records.stream()
                .mapToInt(r -> nullSafe(r.getTotalChars()) - nullSafe(r.getCorrectChars()))
                .sum());
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private Date atStartOfDay(LocalDate day) {
        return Date.from(day.atStartOfDay(DEFAULT_ZONE).toInstant());
    }

    private Date atEndOfDay(LocalDate day) {
        return Date.from(day.atTime(23, 59, 59, 999_999_999).atZone(DEFAULT_ZONE).toInstant());
    }
}
