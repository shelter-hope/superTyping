package com.typinggame.game.service.impl;

import com.typinggame.dto.game.GameRecordSummaryDTO;
import com.typinggame.dto.stats.WeakCharDTO;
import com.typinggame.game.entity.CharacterStats;
import com.typinggame.game.entity.GameRecord;
import com.typinggame.game.repository.CharacterStatsRepository;
import com.typinggame.game.repository.GameRecordRepository;
import com.typinggame.game.service.InternalPlayerDataService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class InternalPlayerDataServiceImpl implements InternalPlayerDataService {

    private static final int MAX_LIMIT = 100;

    private final GameRecordRepository gameRecordRepository;
    private final CharacterStatsRepository characterStatsRepository;

    public InternalPlayerDataServiceImpl(GameRecordRepository gameRecordRepository,
                                         CharacterStatsRepository characterStatsRepository) {
        this.gameRecordRepository = gameRecordRepository;
        this.characterStatsRepository = characterStatsRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameRecordSummaryDTO> recentRecords(Integer userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        List<GameRecord> records = gameRecordRepository.findByUserId(userId, PageRequest.of(0, clamp(limit)));
        return records.stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameRecordSummaryDTO> recordsBetween(Integer userId, Date startDate, Date endDate) {
        if (userId == null || startDate == null || endDate == null) {
            return List.of();
        }
        List<GameRecord> records = gameRecordRepository.findByUserIdAndCreateTimeBetween(userId, startDate, endDate);
        return records.stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeakCharDTO> weakChars(Integer userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        List<CharacterStats> stats = characterStatsRepository
                .findByUserIdOrderByAverageAccuracyAsc(userId, PageRequest.of(0, clamp(limit)));
        return stats.stream().map(this::toWeakChar).toList();
    }

    private GameRecordSummaryDTO toSummary(GameRecord record) {
        return new GameRecordSummaryDTO(
                record.getRecordId(),
                record.getUserId(),
                record.getGameMode(),
                record.getTotalChars(),
                record.getCorrectChars(),
                record.getAvgAccuracy(),
                record.getAvgSpeed(),
                record.getTotalScore(),
                record.getCreateTime());
    }

    private WeakCharDTO toWeakChar(CharacterStats stats) {
        return new WeakCharDTO(
                stats.getUserId(),
                stats.getCharValue(),
                stats.getCharType(),
                stats.getTotalAttempts(),
                stats.getCorrectAttempts(),
                stats.getErrorCount(),
                stats.getAverageAccuracy(),
                stats.getRecentAccuracy());
    }

    private int clamp(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
