package com.typinggame.chat.service;

import com.typinggame.dto.leaderboard.LeaderboardDTO;
import java.util.List;

public interface LeaderboardService {
    List<LeaderboardDTO> getGlobalLeaderboard(String mode, int limit);
    LeaderboardDTO getMyRank(Integer userId, String mode);
    List<LeaderboardDTO> getLeaderboardByPeriod(String mode, String period, int limit);
    List<LeaderboardDTO> searchLeaderboard(String keyword, String mode);
    LeaderboardDTO getUserDetails(Integer userId, String mode);
}
