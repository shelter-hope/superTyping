package com.typinggame.dto.game;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 上传游戏记录后的结算摘要，供前端结算页展示（如当前难度 Dₜ）
 */
@Data
public class GameRecordUploadResultDTO {
    private Integer recordId;
    /** 本局结算后的当前难度 Dₜ（仅当该模式参与难度模型时有值） */
    private BigDecimal currentDifficulty;
}
