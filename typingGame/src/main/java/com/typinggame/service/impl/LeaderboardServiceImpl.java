package com.typinggame.service.impl;

import com.typinggame.dto.leaderboard.LeaderboardDTO;
import com.typinggame.entity.leaderboard.LeaderboardEntry;
import com.typinggame.enums.GameMode;
import com.typinggame.repository.LeaderboardEntryRepository;
import com.typinggame.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LeaderboardServiceImpl implements LeaderboardService {

    private final LeaderboardEntryRepository leaderboardEntryRepository;

    @Override
    public List<LeaderboardDTO> getGlobalLeaderboard(String mode, int limit) {
        // 前端传 endless/dictionary/limit，库中存枚举名 ENDLESS_ADAPT/WORD_SOURCE/LIMIT_TIME；历史数据可能为 client 名，按两种名称查以兼容
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
        
        return entries.stream().map(entry -> {
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
    }

    @Override
    public LeaderboardDTO getMyRank(Integer userId, String mode) {
        // Simple implementation: fetch top 1000 and find user. 
        // Real implementation should use count query or Redis for efficiency.
        List<LeaderboardDTO> leaderboard = getGlobalLeaderboard(mode, 1000);
        for (int i = 0; i < leaderboard.size(); i++) {
            LeaderboardDTO entry = leaderboard.get(i);
            entry.setRank(i + 1); // Set rank on the fly
            if (entry.getUserId().equals(userId)) {
                return entry;
            }
        }
        return null;
    }
}
