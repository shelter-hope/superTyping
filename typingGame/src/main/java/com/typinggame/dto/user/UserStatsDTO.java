package com.typinggame.dto.user;

import lombok.Data;

/**
 * 用户统计响应DTO
 */
@Data
public class UserStatsDTO {
    // 需要修改和扩展
    private Integer userId;
    private String username;
    private Integer totalGames;
    private Double averageAccuracy;
    private Integer totalWordsTyped;
    private Integer totalCharactersTyped;
    private Integer bestScore;
    private String bestGameMode;
    private Integer ranking;

}