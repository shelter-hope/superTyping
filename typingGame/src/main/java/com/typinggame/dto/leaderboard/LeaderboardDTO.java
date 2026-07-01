package com.typinggame.dto.leaderboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
public class LeaderboardDTO {
    private Integer entryId;
    private Integer recordId;
    private Integer userId;
    private String username;
    private String nickname;
    private String avatar;
    private String gameMode;
    private Integer totalScore;
    private Integer roundNum;
    private BigDecimal avgSpeed;
    private BigDecimal peakSpeed;
    private BigDecimal avgAccuracy;
    private BigDecimal peakAccuracy;
    private Integer gameDuration;
    private Date createTime;
    private Integer rank;

    public LeaderboardDTO(Integer userId, String nickname, String avatar, Integer totalScore, BigDecimal avgSpeed, BigDecimal avgAccuracy, Date createTime) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatar = avatar;
        this.totalScore = totalScore;
        this.avgSpeed = avgSpeed;
        this.avgAccuracy = avgAccuracy;
        this.createTime = createTime;
    }
}
