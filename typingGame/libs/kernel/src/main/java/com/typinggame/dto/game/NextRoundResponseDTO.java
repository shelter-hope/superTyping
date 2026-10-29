package com.typinggame.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.math.BigDecimal;

/**
 * 下一轮游戏数据：数学模型输出参数 + 本轮怪物列表（供前端直接开始本轮）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextRoundResponseDTO {
    /** 服务端计划版本，用于确认本轮使用的唯一参数计划。 */
    private Long planVersion;
    /** 本轮控制目标，内部为 0~1 的准确率。 */
    private BigDecimal targetAccuracy;
    /** 本轮调节参数（Dₜ、Vₜ、Nₜ、Lₜ、类型占比） */
    private GameAdjustParam adjustParam;
    /** 本轮怪物列表（未持久化，前端用于本轮对局） */
    private List<MonsterDataDTO> monsters;
}
