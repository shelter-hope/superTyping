package com.typinggame.service;

import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.UserModelParams;

import java.util.List;

/**
 * 处理数学模型相关逻辑
 */
public interface MathModelService {
    /**
     * 获取用户个性化模型参数
     * @param userId 用户ID
     * @param gameMode 游戏模式
     * @return 模型参数
     */
    UserModelParams getUserModelParams(Integer userId, String gameMode);

    /**
     * 获取模型参数调整历史
     * @param userId 用户ID
     * @param gameMode 游戏模式
     * @param limit 限制数量
     * @return 参数调整历史记录
     */
    List<Object> getModelParamsHistory(Integer userId, String gameMode, int limit);

    /**
     * 按模式获取系统默认模型参数（用于用户无 UserModelParams 时兜底，不持久化）
     * @param gameMode 游戏模式
     * @return 从 ModelInitParams 映射的 UserModelParams，无配置时返回 null
     */
    UserModelParams getDefaultParamsForMode(String gameMode);

    /**
     * 获取模型常量（从 model_constants 表取第一条，无则返回默认值，与 Admin 调参一致）
     */
    ModelConstant getModelConstant();
}
