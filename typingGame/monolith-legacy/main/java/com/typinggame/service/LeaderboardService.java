package com.typinggame.service;

import com.typinggame.dto.leaderboard.LeaderboardDTO;
import java.util.List;

/**
 * 处理排行榜相关业务逻辑
 */
public interface LeaderboardService {
    /**
     * 获取全局排行榜
     * @param mode 游戏模式
     * @param limit 限制数量
     * @return 排行榜列表
     */
    List<LeaderboardDTO> getGlobalLeaderboard(String mode, int limit);

    /**
     * 获取我的排名
     * @param userId 用户ID
     * @param mode 游戏模式
     * @return 排名信息（如果未上榜可能返回null或特定对象）
     */
    LeaderboardDTO getMyRank(Integer userId, String mode);
}
