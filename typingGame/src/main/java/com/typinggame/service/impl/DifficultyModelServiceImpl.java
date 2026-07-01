package com.typinggame.service.impl;

import com.typinggame.dto.game.GamePlayerPerformanceDTO;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.user.CharacterStats;
import com.typinggame.entity.user.UserDailyStats;
import com.typinggame.repository.CharacterStatsRepository;
import com.typinggame.repository.GameRecordRepository;
import com.typinggame.repository.UserDailyStatsRepository;
import com.typinggame.service.DifficultyModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 难度模型聚合服务实现：
 * 从 GameRecord、UserDailyStats、CharacterStats 中聚合 A_h、S_hmax、i_t、R_e 等指标。
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DifficultyModelServiceImpl implements DifficultyModelService {

    private static final int HISTORY_GAME_LIMIT = 20;
    private static final int TOP_ERROR_LIMIT = 10;
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    private final GameRecordRepository gameRecordRepository;
    private final CharacterStatsRepository characterStatsRepository;
    private final UserDailyStatsRepository userDailyStatsRepository;

    @Override
    public GamePlayerPerformanceDTO buildAggregatedPerformance(Integer userId, String gameMode) {
        GamePlayerPerformanceDTO dto = new GamePlayerPerformanceDTO();

        // 1. 历史表现：从 GameRecord / UserDailyStats 聚合 A_h、S_hmax
        BigDecimal historyAvgAccuracy = BigDecimal.ZERO;
        BigDecimal historyPeakSpeed = BigDecimal.ZERO;

        // 使用最近 N 场同模式 GameRecord 计算历史均值与峰值
        List<GameRecord> recentRecords = gameRecordRepository.findByUserIdAndGameMode(
                userId, gameMode, PageRequest.of(0, HISTORY_GAME_LIMIT));
        if (!recentRecords.isEmpty()) {
            BigDecimal sumAcc = BigDecimal.ZERO;
            BigDecimal maxSpeed = BigDecimal.ZERO;
            int count = 0;
            for (GameRecord r : recentRecords) {
                if (r.getAvgAccuracy() != null) {
                    sumAcc = sumAcc.add(r.getAvgAccuracy());
                    count++;
                }
                if (r.getPeakSpeed() != null && r.getPeakSpeed().compareTo(maxSpeed) > 0) {
                    maxSpeed = r.getPeakSpeed();
                }
            }
            if (count > 0) {
                historyAvgAccuracy = sumAcc.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
            }
            historyPeakSpeed = maxSpeed;
        } else {
            // 如果没有足够的 GameRecord，则回退到最近一段时间的 UserDailyStats
            LocalDate end = LocalDate.now(DEFAULT_ZONE);
            LocalDate start = end.minusDays(6);
            List<UserDailyStats> dailyStats = userDailyStatsRepository.findByUserIdAndStatDateBetween(userId, start, end);
            if (!dailyStats.isEmpty()) {
                BigDecimal sumAcc = BigDecimal.ZERO;
                BigDecimal maxSpeed = BigDecimal.ZERO;
                int count = 0;
                for (UserDailyStats s : dailyStats) {
                    if (s.getAvgAccuracy() != null) {
                        sumAcc = sumAcc.add(s.getAvgAccuracy());
                        count++;
                    }
                    if (s.getPeakSpeed() != null && s.getPeakSpeed().compareTo(maxSpeed) > 0) {
                        maxSpeed = s.getPeakSpeed();
                    }
                }
                if (count > 0) {
                    historyAvgAccuracy = sumAcc.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
                }
                historyPeakSpeed = maxSpeed;
            }
        }

        dto.setHistoryAvgAccuracy(historyAvgAccuracy);
        dto.setHistoryPeakSpeed(historyPeakSpeed);

        // 2. 字符错误统计：从 CharacterStats 计算 i_t、R_e
        List<CharacterStats> allChars = characterStatsRepository.findByUserId(userId);
        BigDecimal avgCharErrorRate = BigDecimal.ZERO;
        BigDecimal errorCharRatio = BigDecimal.ZERO;
        if (!allChars.isEmpty()) {
            long totalAttempts = 0;
            long totalErrors = 0;
            for (CharacterStats cs : allChars) {
                int attempts = (cs.getTotalAttempts() != null ? cs.getTotalAttempts() : 0);
                int errors = (cs.getErrorCount() != null ? cs.getErrorCount() : 0);
                totalAttempts += attempts;
                totalErrors += errors;
            }
            if (totalAttempts > 0) {
                avgCharErrorRate = BigDecimal.valueOf((double) totalErrors / totalAttempts)
                        .setScale(4, RoundingMode.HALF_UP);
            }

            // R_e：TopK 易错字符占全部错误次数的比例
            List<CharacterStats> topErrorChars =
                    characterStatsRepository.findByUserIdOrderByErrorCountDesc(userId, PageRequest.of(0, TOP_ERROR_LIMIT));
            long topErrors = 0;
            for (CharacterStats cs : topErrorChars) {
                topErrors += (cs.getErrorCount() != null ? cs.getErrorCount() : 0);
            }
            if (totalErrors > 0 && topErrors > 0) {
                errorCharRatio = BigDecimal.valueOf((double) topErrors / totalErrors)
                        .setScale(4, RoundingMode.HALF_UP);
            }
        }

        dto.setAvgCharErrorRate(avgCharErrorRate);
        dto.setErrorCharRatio(errorCharRatio);

        // 3. 轮次 K 由调用方的最新 GameRecord 或运行时状态决定，这里不做聚合，留给上层填充
        return dto;
    }
}

