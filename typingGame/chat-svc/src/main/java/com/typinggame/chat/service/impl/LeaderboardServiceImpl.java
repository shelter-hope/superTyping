package com.typinggame.chat.service.impl;

import com.typinggame.chat.repository.LeaderboardEntryRepository;
import com.typinggame.chat.service.LeaderboardService;
import com.typinggame.dto.leaderboard.LeaderboardDTO;
import com.typinggame.entity.leaderboard.LeaderboardEntry;
import com.typinggame.enums.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LeaderboardServiceImpl implements LeaderboardService {

    private final LeaderboardEntryRepository leaderboardEntryRepository;

    @Override
    public List<LeaderboardDTO> getGlobalLeaderboard(String mode, int limit) {
        String safeMode = (mode != null && !mode.trim().isEmpty()) ? mode.trim() : "endless";
        String dbMode = GameMode.fromClientMode(safeMode).name();
        List<String> modes = new ArrayList<>();
        modes.add(dbMode);
        if (!dbMode.equalsIgnoreCase(safeMode)) {
            modes.add(safeMode);
        }
        List<LeaderboardEntry> entries = modes.size() > 1
                ? leaderboardEntryRepository.findByGameModeInOrderByTotalScoreDesc(modes, PageRequest.of(0, limit))
                : leaderboardEntryRepository.findByGameModeOrderByTotalScoreDesc(dbMode, PageRequest.of(0, limit));

        List<LeaderboardDTO> result = entries.stream().map(entry -> {
            LeaderboardDTO dto = new LeaderboardDTO();
            dto.setEntryId(entry.getEntryId());
            dto.setRecordId(entry.getRecordId());
            dto.setUserId(entry.getUserId());
            dto.setUsername(entry.getUsername());
            dto.setNickname(entry.getNickname());
            dto.setAvatar(entry.getAvatar());
            dto.setGameMode(entry.getGameMode());
            dto.setTotalScore(entry.getTotalScore());
            dto.setRoundNum(entry.getRoundNum());
            dto.setAvgSpeed(entry.getAvgSpeed());
            dto.setPeakSpeed(entry.getPeakSpeed());
            dto.setAvgAccuracy(entry.getAvgAccuracy());
            dto.setPeakAccuracy(entry.getPeakAccuracy());
            dto.setGameDuration(entry.getGameDuration());
            dto.setCreateTime(entry.getCreateTime());
            return dto;
        }).collect(Collectors.toList());
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }
        return result;
    }

    @Override
    public LeaderboardDTO getMyRank(Integer userId, String mode) {
        List<LeaderboardDTO> leaderboard = getGlobalLeaderboard(mode, 1000);
        for (int i = 0; i < leaderboard.size(); i++) {
            LeaderboardDTO entry = leaderboard.get(i);
            entry.setRank(i + 1);
            if (entry.getUserId().equals(userId)) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public List<LeaderboardDTO> getLeaderboardByPeriod(String mode, String period, int limit) {
        List<LeaderboardDTO> all = getGlobalLeaderboard(mode, Math.max(limit, 1000));
        Date start = resolvePeriodStart(period);
        if (start == null) {
            return all.stream().limit(limit).collect(Collectors.toList());
        }
        return all.stream()
                .filter(item -> item.getCreateTime() != null && !item.getCreateTime().before(start))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaderboardDTO> searchLeaderboard(String keyword, String mode) {
        String q = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return new ArrayList<>();
        }
        return getGlobalLeaderboard(mode, 1000).stream()
                .filter(item -> contains(item.getUsername(), q)
                        || contains(item.getNickname(), q)
                        || String.valueOf(item.getUserId()).equals(q))
                .limit(50)
                .collect(Collectors.toList());
    }

    @Override
    public LeaderboardDTO getUserDetails(Integer userId, String mode) {
        if (userId == null) {
            return null;
        }
        return getGlobalLeaderboard(mode, 1000).stream()
                .filter(item -> userId.equals(item.getUserId()))
                .findFirst()
                .orElse(null);
    }

    private Date resolvePeriodStart(String period) {
        if (period == null || "total".equalsIgnoreCase(period)) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;
        if ("week".equalsIgnoreCase(period)) {
            start = now.minusDays(7);
        } else if ("month".equalsIgnoreCase(period)) {
            start = now.minusMonths(1);
        } else {
            return null;
        }
        return Date.from(start.atZone(ZoneId.systemDefault()).toInstant());
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
