package com.typinggame.dto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户模型适配日志（列表/查询用，避免实体关联序列化）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserModelAdaptLogDTO {
    private Integer logId;
    private Integer userId;
    private Integer gameRecordId;
    private String gameMode;
    private Integer roundNum;
    private BigDecimal beforeDifficulty;
    private BigDecimal afterDifficulty;
    private String adjustParams;
    private String triggerRule;
    private LocalDateTime logTime;
}
