package com.typinggame.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 跨服务用户公开档案（user-svc 为唯一来源）。
 * 只暴露展示与统计需要的字段，绝不包含密码等敏感信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Integer userId;
    private String username;
    private String nickname;
    private String signature;
    private String avatar;
    /** 1 正常 / 0 禁用，null 视为正常 */
    private Integer status;
    private Boolean enabled;
    private Integer bestScore;
    private Integer bestRound;
    private BigDecimal bestSpeed;
    private BigDecimal bestAccuracy;
    private BigDecimal avgSpeed;
    private BigDecimal avgAccuracy;
    private BigDecimal peakAccuracy;
    private Integer gameNum;
}
