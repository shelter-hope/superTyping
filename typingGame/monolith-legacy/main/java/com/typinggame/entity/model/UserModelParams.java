package com.typinggame.entity.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户个性化数学模型参数表
 */
@Entity
@Table(name = "user_model_params")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class  UserModelParams {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer paramId; // 主键ID
    private Integer userId; // 关联用户ID
    private String gameMode;  // 游戏模式
    private Integer latestRound; // 最近一次更新的轮次

    private BigDecimal lastDifficulty; // 上一轮的难度系数

    private BigDecimal weightRealAccuracy; // 实时准确率权重WA
    private BigDecimal weightRealSpeed; // 实时速度权重WS
    private BigDecimal weightCharErrorRate; // 单字符错误率权重WE
    private BigDecimal weightHistoryAccuracy; // 历史平均准确率权重WAh
    private BigDecimal weightHistoryPeakSpeed; // 历史峰值速度权重WSh
    private BigDecimal weightErrorCharRatio; // 易错字符占比权重WRe
    private BigDecimal weightCurrentRound; // 轮次权重WK

    private Boolean fatigueBufferFlag; // 疲劳缓冲标记（0=未触发/1=已触发）
    private Boolean progressSpeedFlag; // 进步速度标记（0=未触发/1=已触发）
    private Boolean rollbackFlag; // 回退机制标记（0=未触发/1=已触发）

    private BigDecimal maxAdjustRatio; // 单次调节幅度上限（30%）
    private BigDecimal smoothAdjustCoeff; // 平滑调节系数α
    
    // 遗传进化参数
    private BigDecimal crossoverRate; // 交叉概率
    private BigDecimal mutationRate; // 变异概率
    
    // 参数调节系数
    private BigDecimal speedAdjustCoeff; // 速度调节系数beta
    private BigDecimal lengthAdjustCoeff; // 长度调节系数gamma
    
    private Integer consecutiveMismatchRound; // 连续难度不匹配轮次
    private Date lastAdjustTime; // 最后一次参数调整时间
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间
}