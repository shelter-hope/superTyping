package com.typinggame.service;

import com.typinggame.dto.stats.CharacterStatsBatchUpdateDTO;
import com.typinggame.entity.user.CharacterStats;
import java.util.List;

/**
 * 处理字符统计相关业务逻辑
 */
public interface CharacterStatsService {
    /**
     * 批量更新字符统计
     * @param userId 用户ID
     * @param batchDTO 批量更新数据
     */
    void batchUpdate(Integer userId, CharacterStatsBatchUpdateDTO batchDTO);

    /**
     * 获取用户所有字符统计
     * @param userId 用户ID
     * @return 统计列表
     */
    List<CharacterStats> getUserStats(Integer userId);

    /**
     * 获取用户易错字符（按错误次数排序）
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 易错字符列表
     */
    List<CharacterStats> getWeakChars(Integer userId, int limit);

    /**
     * 获取用户准确率最低的字符
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 低准确率字符列表
     */
    List<CharacterStats> getLowAccuracyChars(Integer userId, int limit);
}
