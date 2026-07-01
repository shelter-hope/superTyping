package com.typinggame.service;

import com.typinggame.dto.game.GamePlayerPerformanceDTO;

/**
 * 难度模型聚合服务：
 * 从 GameRecord / UserDailyStats / CharacterStats 等表中聚合出
 * A_h、S_hmax、i_t、R_e 等指标，为综合难度系数 D_t 计算提供输入。
 */
public interface DifficultyModelService {

    /**
     * 为指定用户 + 模式构建一份玩家表现指标（实时+历史）的聚合结果。
     * <p>
     * 约定：
     * - realAccuracy / realSpeed 由最近一局（或调用方传入）提供；
     * - historyAvgAccuracy / historyPeakSpeed / avgCharErrorRate / errorCharRatio
     *   从历史记录与字符统计表中聚合得到。
     *
     * @param userId   用户 ID
     * @param gameMode 游戏模式（使用后端枚举名，如 ENDLESS_ADAPT / WORD_SOURCE）
     * @return 聚合后的玩家表现指标，查无数据时字段可能为 null
     */
    GamePlayerPerformanceDTO buildAggregatedPerformance(Integer userId, String gameMode);
}

