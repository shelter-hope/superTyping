package com.typinggame.game.service.impl;

import com.typinggame.dto.stats.ErrorComparisonDTO;
import com.typinggame.dto.stats.TrainingSuggestionDTO;
import com.typinggame.dto.stats.TrendPointDTO;
import com.typinggame.dto.stats.TypingBehaviorDTO;
import com.typinggame.game.entity.GameRecord;
import com.typinggame.game.entity.CharacterStats;
import com.typinggame.game.repository.CharacterStatsRepository;
import com.typinggame.game.repository.GameRecordRepository;
import com.typinggame.game.service.StatsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatsServiceImpl implements StatsService {

    private static final int TREND_PAGE_SIZE = 500;
    private static final int TOP_ERROR_N = 15;
    private static final int SUGGESTION_CHARS = 10;
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    private final GameRecordRepository gameRecordRepository;
    private final CharacterStatsRepository characterStatsRepository;

    public StatsServiceImpl(GameRecordRepository gameRecordRepository, CharacterStatsRepository characterStatsRepository) {
        this.gameRecordRepository = gameRecordRepository;
        this.characterStatsRepository = characterStatsRepository;
    }

    @Override
    public TypingBehaviorDTO analyzeTypingBehavior(Integer userId) {
        List<GameRecord> records = gameRecordRepository.findByUserId(userId, PageRequest.of(0, TREND_PAGE_SIZE));
        List<CharacterStats> charStats = characterStatsRepository.findByUserId(userId);

        double avgSpeed = records.stream().mapToDouble(r -> toDouble(r.getAvgSpeed())).average().orElse(0);
        double avgAccuracy = records.stream().mapToDouble(r -> toDouble(r.getAvgAccuracy())).average().orElse(0);
        int totalChars = records.stream().mapToInt(r -> nullSafeInt(r.getTotalChars())).sum();
        int totalErrors = records.stream().mapToInt(r -> nullSafeInt(r.getTotalChars()) - nullSafeInt(r.getCorrectChars())).sum();
        List<Double> recentSpeeds = records.stream().map(r -> toDouble(r.getAvgSpeed())).limit(30).collect(Collectors.toList());
        Map<String, Integer> errorByType = new HashMap<>();
        for (CharacterStats cs : charStats) {
            if (nullSafeInt(cs.getErrorCount()) > 0) {
                errorByType.merge(cs.getCharType() == null ? "other" : cs.getCharType(), cs.getErrorCount(), Integer::sum);
            }
        }
        List<TypingBehaviorDTO.CharErrorItem> topErrors = characterStatsRepository
                .findByUserIdOrderByErrorCountDesc(userId, PageRequest.of(0, TOP_ERROR_N))
                .stream()
                .map(cs -> new TypingBehaviorDTO.CharErrorItem(cs.getCharValue(), cs.getErrorCount(), toDoubleOrNull(cs.getAverageAccuracy())))
                .collect(Collectors.toList());

        double speedStdDev = 0;
        if (!recentSpeeds.isEmpty()) {
            double avgSpeedFinal = avgSpeed;
            speedStdDev = Math.sqrt(recentSpeeds.stream().mapToDouble(s -> Math.pow(s - avgSpeedFinal, 2)).average().orElse(0));
        }
        return TypingBehaviorDTO.builder()
                .avgSpeed(round(avgSpeed, 1))
                .speedStdDev(round(speedStdDev, 2))
                .avgAccuracy(round(avgAccuracy, 1))
                .totalGames(records.size())
                .totalChars(totalChars)
                .totalErrors(Math.max(0, totalErrors))
                .errorDistributionByType(errorByType)
                .topErrorChars(topErrors)
                .recentSpeeds(recentSpeeds)
                .build();
    }

    @Override
    public List<TrendPointDTO> getProgressTrend(Integer userId, String period) {
        return buildTrendFromRecords(userId, normalizeTimeRange(period));
    }

    @Override
    public TrainingSuggestionDTO getTrainingSuggestions(Integer userId) {
        List<CharacterStats> weak = characterStatsRepository.findByUserIdOrderByAverageAccuracyAsc(userId, PageRequest.of(0, SUGGESTION_CHARS));
        List<GameRecord> recent = gameRecordRepository.findByUserId(userId, PageRequest.of(0, 20));
        double recentAccuracy = recent.stream().mapToDouble(r -> toDouble(r.getAvgAccuracy())).average().orElse(0);
        double recentSpeed = recent.stream().mapToDouble(r -> toDouble(r.getAvgSpeed())).average().orElse(0);
        List<TrainingSuggestionDTO.CharSuggestionItem> focus = weak.stream()
                .map(cs -> new TrainingSuggestionDTO.CharSuggestionItem(cs.getCharValue(), cs.getErrorCount(), toDoubleOrNull(cs.getAverageAccuracy()), cs.getCharType()))
                .collect(Collectors.toList());
        String summary = focus.isEmpty()
                ? "暂无易错字符数据，多玩几局后会给出针对性建议。"
                : "建议重点练习以下字符：" + focus.stream().map(TrainingSuggestionDTO.CharSuggestionItem::getCharacter).limit(5).collect(Collectors.joining("、"));
        return TrainingSuggestionDTO.builder()
                .focusChars(focus)
                .suggestedMode("endless")
                .summary(summary)
                .recentAccuracy(round(recentAccuracy, 1))
                .recentSpeed(round(recentSpeed, 1))
                .build();
    }

    @Override
    public List<TrendPointDTO> getTrendData(Integer userId, String timeRange) {
        return buildTrendFromRecords(userId, normalizeTimeRange(timeRange));
    }

    @Override
    public List<TrendPointDTO> getSpeedTrend(Integer userId, String timeRange) {
        return getTrendData(userId, timeRange);
    }

    @Override
    public List<TrendPointDTO> getAccuracyTrend(Integer userId, String timeRange) {
        return getTrendData(userId, timeRange);
    }

    @Override
    public ErrorComparisonDTO getErrorComparison(Integer userId) {
        LocalDate now = LocalDate.now(DEFAULT_ZONE);
        LocalDate thisWeekStart = now.minusDays(6);
        LocalDate lastWeekStart = now.minusDays(13);
        List<GameRecord> thisWeek = gameRecordRepository.findByUserIdAndCreateTimeBetween(userId, atStartOfDay(thisWeekStart), atEndOfDay(now));
        List<GameRecord> lastWeek = gameRecordRepository.findByUserIdAndCreateTimeBetween(userId, atStartOfDay(lastWeekStart), atStartOfDay(thisWeekStart));
        int thisWeekErrors = countErrors(thisWeek);
        int lastWeekErrors = countErrors(lastWeek);
        int change = thisWeekErrors - lastWeekErrors;
        double changeRate = lastWeekErrors == 0 ? (thisWeekErrors == 0 ? 0 : 1) : (double) change / lastWeekErrors;
        List<ErrorComparisonDTO.CharErrorItem> top = characterStatsRepository.findByUserIdOrderByErrorCountDesc(userId, PageRequest.of(0, 10))
                .stream()
                .map(cs -> new ErrorComparisonDTO.CharErrorItem(cs.getCharValue(), cs.getErrorCount(), toDoubleOrNull(cs.getAverageAccuracy())))
                .collect(Collectors.toList());
        return ErrorComparisonDTO.builder()
                .thisWeekErrors(thisWeekErrors)
                .lastWeekErrors(lastWeekErrors)
                .change(change)
                .changeRate(round(changeRate, 2))
                .thisWeekTopErrors(top)
                .lastWeekTopErrors(top)
                .build();
    }

    private List<TrendPointDTO> buildTrendFromRecords(Integer userId, String range) {
        LocalDate end = LocalDate.now(DEFAULT_ZONE);
        LocalDate start = switch (range) {
            case "3days" -> end.minusDays(2);
            case "month" -> end.minusDays(29);
            case "year" -> end.minusDays(364);
            default -> end.minusDays(6);
        };
        List<GameRecord> records = gameRecordRepository.findByUserIdAndCreateTimeBetween(userId, atStartOfDay(start), atEndOfDay(end));
        if (records.isEmpty()) return Collections.emptyList();

        Map<String, List<GameRecord>> grouped = new LinkedHashMap<>();
        for (GameRecord record : records) {
            if (record.getCreateTime() == null) continue;
            LocalDate day = record.getCreateTime().toInstant().atZone(DEFAULT_ZONE).toLocalDate();
            String key = switch (range) {
                case "month" -> day.with(java.time.DayOfWeek.MONDAY).toString();
                case "year" -> day.getYear() + "-" + String.format("%02d", day.getMonthValue());
                default -> day.toString();
            };
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }
        List<TrendPointDTO> result = new ArrayList<>();
        grouped.forEach((label, list) -> result.add(new TrendPointDTO(
                label,
                round(list.stream().mapToDouble(r -> toDouble(r.getAvgSpeed())).average().orElse(0), 1),
                round(list.stream().mapToDouble(r -> toDouble(r.getPeakSpeed())).max().orElse(0), 1),
                round(list.stream().mapToDouble(r -> toDouble(r.getAvgAccuracy())).average().orElse(0), 1),
                round(list.stream().mapToDouble(r -> toDouble(r.getPeakAccuracy())).max().orElse(0), 1),
                list.size(),
                countErrors(list)
        )));
        result.sort(Comparator.comparing(TrendPointDTO::getLabel));
        return result;
    }

    private String normalizeTimeRange(String timeRange) {
        if (timeRange == null) return "week";
        return switch (timeRange.toLowerCase()) {
            case "3days", "three-days" -> "3days";
            case "month" -> "month";
            case "year", "all" -> "year";
            default -> "week";
        };
    }

    private static int countErrors(List<GameRecord> records) {
        return Math.max(0, records.stream().mapToInt(r -> nullSafeInt(r.getTotalChars()) - nullSafeInt(r.getCorrectChars())).sum());
    }

    private static int nullSafeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static double toDouble(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }

    private static Double toDoubleOrNull(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private static Date atStartOfDay(LocalDate day) {
        return Date.from(day.atStartOfDay(DEFAULT_ZONE).toInstant());
    }

    private static Date atEndOfDay(LocalDate day) {
        return Date.from(day.atTime(23, 59, 59, 999_999_999).atZone(DEFAULT_ZONE).toInstant());
    }

    private static double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
