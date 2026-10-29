package com.typinggame.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 对局记录摘要（跨服务读契约）。
 *
 * <p>由 game-core 的内部接口返回，供 ai-svc 等只读消费方使用；字段只暴露跨服务确实需要的部分，
 * 避免把对局表结构整体透出。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRecordSummaryDTO {

    private Integer recordId;
    private Integer userId;
    private String gameMode;
    private Integer totalChars;
    private Integer correctChars;
    private BigDecimal avgAccuracy;
    private BigDecimal avgSpeed;
    private Integer totalScore;
    private Date createTime;
}
