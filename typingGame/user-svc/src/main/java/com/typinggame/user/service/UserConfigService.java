package com.typinggame.user.service;

import com.typinggame.entity.config.UserAppConfig;

import java.util.Map;

public interface UserConfigService {
    UserAppConfig getConfig(Integer userId);

    UserAppConfig saveConfig(Integer userId, Map<String, Object> updates);

    boolean updateSoundSettings(Integer userId, Boolean enabled);
}
