package com.typinggame.service.impl;

import com.typinggame.dto.stats.ErrorComparisonDTO;
import com.typinggame.dto.stats.TrainingSuggestionDTO;
import com.typinggame.dto.stats.TrendPointDTO;
import com.typinggame.dto.stats.TypingBehaviorDTO;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.user.CharacterStats;
import com.typinggame.repository.CharacterStatsRepository;
import com.typinggame.repository.GameRecordRepository;
import com.typinggame.service.StatsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计分析服务实现：基于 GameRecord、CharacterStats 聚合
 */
@Slf4j
@Service
public class StatsServiceImpl implements StatsService {

    private static final int TREND_PAGE_SIZE = 500;
    private static final int TOP_ERROR_N = 15;
    private static final int SUGGESTION_CHARS = 10;
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    @Autowired
    private GameRecordRepository gameRecordRepository;
    @Autowired
    private CharacterStatsRepository characterStatsRepository;

    @Override
    public TypingBehaviorDTO analyzeTypingBehavior(Integer userId) {
        List<GameRecord> records = gameRecordRepository.findByUserId(userId, PageRequest.of(0, TREND_PAGE_SIZE));
        List<CharacterStats> charStats = characterStatsRepository.findByUserId(userId);

        double avgSpeed = 0, avgAccuracy = 0;
        int totalGames = records.size();
        int totalChars = 0, totalErrors = 0;
        List<Double> recentSpeeds = new ArrayList<>();
        Map<String, Integer> errorByType = new HashMap<>();
        List<TypingBehaviorDTO.CharErrorItem> topErrors = new ArrayList<>();

        if (!records.isEmpty()) {
            avgSpeed = records.stream()
                    .map(r -> r.getAvgSpeed() != null ? r.getAvgSpeed().doubleValue() : 0d)
                    .mapToDouble(Double::doubleValue).average().orElse(0);
            avgAccuracy = records.stream()
                    .map(r -> r.getAvgAccuracy() != null ? r.getAvgAccuracy().doubleValue() : 0d)
                    .mapToDouble(Double::doubleValue).average().orElse(0);
            for (GameRecord r : records) {
                totalChars += r.getTotalChars() != null ? r.getTotalChars() : 0;
                totalErrors += (r.getTotalChars() != null && r.getCorrectChars() != null)
                        ? (r.getTotalChars() - r.getCorrectChars()) : 0;
                if (r.getAvgSpeed() != null) recentSpeeds.add(r.getAvgSpeed().doubleValue());
            }
        }

        for (CharacterStats cs : charStats) {
            if (cs.getErrorCount() != null && cs.getErrorCount() > 0) {
                String type = cs.getCharType() != null ? cs.getCharType() : "other";
                errorByType.merge(type, cs.getErrorCount(), Integer::sum);
            }
        }
        characterStatsRepository.findByUserIdOrderByErrorCountDesc(userId, PageRequest.of(0, TOP_ERROR_N))
                .forEach(cs -> topErrors.add(new TypingBehaviorDTO.CharErrorItem(
                        cs.getCharValue(),
                        cs.getErrorCount(),
                        cs.getAverageAccuracy() != null ? cs.getAverageAccuracy().doubleValue() : null
                )));

        final double avgSpeedFinal = avgSpeed;
        return TypingBehaviorDTO.builder()
                .avgSpeed(round(avgSpeed, 1))
                .speedStdDev(records.isEmpty() ? 0 : round(Math.sqrt(recentSpeeds.stream().mapToDouble(s -> Math.pow(s - avgSpeedFinal, 2)).average().orElse(0)), 2))
                .avgAccuracy(round(avgAccuracy, 1))
                .totalGames(totalGames)
                .totalChars(totalChars)
                .totalErrors(totalErrors)
                .errorDistributionByType(errorByType)
                .topErrorChars(topErrors)
                .recentSpeeds(recentSpeeds.size() > 30 ? recentSpeeds.subList(0, 30) : recentSpeeds)
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
        double recentAccuracy = 0, recentSpeed = 0;
        if (!recent.isEmpty()) {
            recentAccuracy = recent.stream().map(r -> r.getAvgAccuracy() != null ? r.getAvgAccuracy().doubleValue() : 0d).mapToDouble(Double::doubleValue).average().orElse(0);
            recentSpeed = recent.stream().map(r -> r.getAvgSpeed() != null ? r.getAvgSpeed().doubleValue() : 0d).mapToDouble(Double::doubleValue).average().orElse(0);
        }
        List<TrainingSuggestionDTO.CharSuggestionItem> focus = weak.stream()
                .map(cs -> new TrainingSuggestionDTO.CharSuggestionItem(
                        cs.getCharValue(),
                        cs.getErrorCount(),
                        cs.getAverageAccuracy() != null ? cs.getAverageAccuracy().doubleValue() : null,
                        cs.getCharType()
                ))
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
        Date startThis = atStartOfDay(thisWeekStart);
        Date endThis = atEndOfDay(now);
        Date startLast = atStartOfDay(lastWeekStart);
        Date endLast = atStartOfDay(thisWeekStart);

        List<GameRecord> thisWeek = gameRecordRepository.findByUserIdAndCreateTimeBetween(userId, startThis, endThis);
        List<GameRecord> lastWeek = gameRecordRepository.findByUserIdAndCreateTimeBetween(userId, startLast, endLast);
        int thisWeekErrors = thisWeek.stream().mapToInt(r -> (r.getTotalChars() != null && r.getCorrectChars() != null) ? (r.getTotalChars() - r.getCorrectChars()) : 0).sum();
        int lastWeekErrors = lastWeek.stream().mapToInt(r -> (r.getTotalChars() != null && r.getCorrectChars() != null) ? (r.getTotalChars() - r.getCorrectChars()) : 0).sum();
        int change = thisWeekErrors - lastWeekErrors;
        double changeRate = lastWeekErrors == 0 ? (thisWeekErrors == 0 ? 0 : 1) : (double) change / lastWeekErrors;

        List<ErrorComparisonDTO.CharErrorItem> thisTop = characterStatsRepository.findByUserIdOrderByErrorCountDesc(userId, PageRequest.of(0, 10))
                .stream().map(cs -> new ErrorComparisonDTO.CharErrorItem(cs.getCharValue(), cs.getErrorCount(), cs.getAverageAccuracy() != null ? cs.getAverageAccuracy().doubleValue() : null))
                .collect(Collectors.toList());
        List<ErrorComparisonDTO.CharErrorItem> lastTop = thisTop;
        return ErrorComparisonDTO.builder()
                .thisWeekErrors(thisWeekErrors)
                .lastWeekErrors(lastWeekErrors)
                .change(change)
                .changeRate(round(changeRate, 2))
                .thisWeekTopErrors(thisTop)
                .lastWeekTopErrors(lastTop)
                .build();
    }

    private String normalizeTimeRange(String timeRange) {
        if (timeRange == null) return "week";
        switch (timeRange.toLowerCase()) {
            case "3days": case "three-days": return "3days";
            case "month": return "month";
            case "year": return "year";
            case "all": return "year";
            default: return "week";
        }
    }

    private List<TrendPointDTO> buildTrendFromRecords(Integer userId, String range) {
        LocalDate end = LocalDate.now(DEFAULT_ZONE);
        LocalDate start;
        boolean byDay, byWeek, byMonth;
        switch (range) {
            case "3days":
                start = end.minusDays(2);
                byDay = true; byWeek = false; byMonth = false;
                break;
            case "month":
                start = end.minusDays(29);
                byDay = false; byWeek = true; byMonth = false;
                break;
            case "year":
                start = end.minusDays(364);
                byDay = false; byWeek = false; byMonth = true;
                break;
            default:
                start = end.minusDays(6);
                byDay = true; byWeek = false; byMonth = false;
        }
        Date startDate = atStartOfDay(start);
        Date endDate = atEndOfDay(end);
        List<GameRecord> records = gameRecordRepository.findByUserIdAndCreateTimeBetween(userId, startDate, endDate);
        if (records.isEmpty()) return Collections.emptyList();

        Map<String, List<GameRecord>> grouped = new LinkedHashMap<>();
        for (GameRecord r : records) {
            LocalDate d = r.getCreateTime().toInstant().atZone(DEFAULT_ZONE).toLocalDate();
            String key;
            if (byDay) key = d.toString();
            else if (byWeek) key = d.with(java.time.DayOfWeek.MONDAY).toString();
            else key = d.getYear() + "-" + String.format("%02d", d.getMonthValue());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        List<TrendPointDTO> result = new ArrayList<>();
        grouped.forEach((label, list) -> {
            double avgSpeed = list.stream().map(r -> r.getAvgSpeed() != null ? r.getAvgSpeed().doubleValue() : 0d).mapToDouble(Double::doubleValue).average().orElse(0);
            double peakSpeed = list.stream().map(r -> r.getPeakSpeed() != null ? r.getPeakSpeed().doubleValue() : 0d).mapToDouble(Double::doubleValue).max().orElse(0);
            double avgAcc = list.stream().map(r -> r.getAvgAccuracy() != null ? r.getAvgAccuracy().doubleValue() : 0d).mapToDouble(Double::doubleValue).average().orElse(0);
            double peakAcc = list.stream().map(r -> r.getPeakAccuracy() != null ? r.getPeakAccuracy().doubleValue() : 0d).mapToDouble(Double::doubleValue).max().orElse(0);
            int errors = list.stream().mapToInt(r -> (r.getTotalChars() != null && r.getCorrectChars() != null) ? (r.getTotalChars() - r.getCorrectChars()) : 0).sum();
            result.add(new TrendPointDTO(label, round(avgSpeed, 1), round(peakSpeed, 1), round(avgAcc, 1), round(peakAcc, 1), list.size(), errors));
        });
        result.sort(Comparator.comparing(TrendPointDTO::getLabel));
        return result;
    }

    private static Date atStartOfDay(LocalDate d) {
        return Date.from(d.atStartOfDay(DEFAULT_ZONE).toInstant());
    }

    private static Date atEndOfDay(LocalDate d) {
        return Date.from(d.atTime(23, 59, 59, 999_999_999).atZone(DEFAULT_ZONE).toInstant());
    }

    private static double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
