package com.typinggame.service;

import com.typinggame.entity.user.UserInfo;

/**
 * 新用户注册后基础表初始化服务。
 * 负责写入 user_app_configs、user_model_params、user_game_mode_configs 等默认配置。
 */
public interface UserRegistrationInitService {

    /**
     * 为新注册用户初始化所有基础配置表。
     * 任一步失败会记录日志，不抛异常，保证注册流程可正常返回。
     *
     * @param user 已持久化的新用户实体
     */
    void initForNewUser(UserInfo user);
}
