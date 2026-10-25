package com.typinggame.game.service;

/** 新用户对局侧配置初始化。写所有权在 game-core。 */
public interface UserBootstrapService {
    void bootstrapUser(Integer userId);
}
