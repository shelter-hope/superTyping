package com.typinggame.dto.game;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 游戏调节目标参数（输出变量）
 * 对应文档：基础参数-调节目标参数
 */
@Data
public class GameAdjustParam {
    // 怪物速度：基础行进速度（像素/帧），范围[0.8,3.0]，调节粒度0.1
    private BigDecimal monsterSpeed;

    // 怪物数量：单轮生成数量，范围[3,25]，调节粒度1
    private Integer monsterCount;

    // 字符串长度：平均字符长度，范围[1,15]，调节粒度1
    private Integer avgStringLength;

    // 怪物类型占比（总和100%）
    private Integer normalRatio;  // 普通型占比Pn（30%-80%）
    private Integer attackRatio;  // 攻击型占比Pa（0%-20%）
    private Integer evasiveRatio; // 回避型占比Pe（5%-25%）
    private Integer smartRatio;   // 智慧型占比Ps（5%-25%）

    // 扩展字段：当前轮次综合难度系数
    private BigDecimal currentDifficulty;

}
