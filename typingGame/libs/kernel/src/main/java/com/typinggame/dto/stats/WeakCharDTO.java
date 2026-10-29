package com.typinggame.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 弱项字符（跨服务读契约）。
 *
 * <p>由 game-core 的内部接口返回，供 ai-svc 等只读消费方生成训练建议。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeakCharDTO {

    private Integer userId;
    private String charValue;
    private String charType;
    private Integer totalAttempts;
    private Integer correctAttempts;
    private Integer errorCount;
    private BigDecimal averageAccuracy;
    private BigDecimal recentAccuracy;
}
