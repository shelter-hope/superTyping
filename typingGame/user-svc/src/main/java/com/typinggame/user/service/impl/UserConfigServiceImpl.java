package com.typinggame.user.service.impl;

import com.typinggame.user.entity.UserAppConfig;
import com.typinggame.user.repository.UserAppConfigRepository;
import com.typinggame.user.service.UserConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

@Service
public class UserConfigServiceImpl implements UserConfigService {

    private final UserAppConfigRepository userAppConfigRepository;

    public UserConfigServiceImpl(UserAppConfigRepository userAppConfigRepository) {
        this.userAppConfigRepository = userAppConfigRepository;
    }

    @Override
    public UserAppConfig getConfig(Integer userId) {
        return userAppConfigRepository.findByUserId(userId).orElseGet(() -> createDefaultConfig(userId));
    }

    @Override
    @Transactional
    public UserAppConfig saveConfig(Integer userId, Map<String, Object> updates) {
        UserAppConfig config = getConfig(userId);
        if (updates == null || updates.isEmpty()) {
            return config;
        }
        applyUpdates(config, updates);
        config.setUpdateTime(new Date());
        return userAppConfigRepository.save(config);
    }

    @Override
    @Transactional
    public boolean updateSoundSettings(Integer userId, Boolean enabled) {
        if (enabled == null) {
            throw new IllegalArgumentException("音效开关不能为空");
        }
        UserAppConfig config = getConfig(userId);
        config.setIsMusic(enabled);
        config.setIsSoundEffect(enabled);
        config.setUpdateTime(new Date());
        userAppConfigRepository.save(config);
        return true;
    }

    private UserAppConfig createDefaultConfig(Integer userId) {
        Date now = new Date();
        UserAppConfig config = new UserAppConfig();
        config.setUserId(userId);
        config.setIsMusic(true);
        config.setIsSoundEffect(true);
        config.setShowGameRecords(true);
        config.setShowRankings(true);
        config.setIsSearchable(true);
        config.setDarkMode(false);
        config.setNotificationsEnabled(true);
        config.setAutoSave(true);
        config.setLanguage("zh-CN");
        config.setFontSize(16);
        config.setTheme("default");
        config.setCreateTime(now);
        config.setUpdateTime(now);
        return userAppConfigRepository.save(config);
    }

    private void applyUpdates(UserAppConfig config, Map<String, Object> updates) {
        setBoolean(updates, "isMusic", config::setIsMusic);
        setBoolean(updates, "isSoundEffect", config::setIsSoundEffect);
        setBoolean(updates, "soundEnabled", value -> {
            config.setIsMusic(value);
            config.setIsSoundEffect(value);
        });
        setBoolean(updates, "showGameRecords", config::setShowGameRecords);
        setBoolean(updates, "showRankings", config::setShowRankings);
        setBoolean(updates, "isSearchable", config::setIsSearchable);
        setBoolean(updates, "darkMode", config::setDarkMode);
        setBoolean(updates, "notificationsEnabled", config::setNotificationsEnabled);
        setBoolean(updates, "autoSave", config::setAutoSave);
        setString(updates, "language", config::setLanguage, 10);
        setString(updates, "theme", config::setTheme, 20);
        Object fontSize = updates.get("fontSize");
        if (fontSize instanceof Number number) {
            int value = number.intValue();
            if (value < 12 || value > 32) {
                throw new IllegalArgumentException("字体大小必须在12到32之间");
            }
            config.setFontSize(value);
        }
    }

    private void setBoolean(Map<String, Object> updates, String key, java.util.function.Consumer<Boolean> setter) {
        if (updates.containsKey(key)) {
            Object value = updates.get(key);
            if (!(value instanceof Boolean bool)) {
                throw new IllegalArgumentException(key + " 必须是布尔值");
            }
            setter.accept(bool);
        }
    }

    private void setString(Map<String, Object> updates, String key, java.util.function.Consumer<String> setter, int maxLength) {
        if (updates.containsKey(key)) {
            Object value = updates.get(key);
            if (value == null) return;
            String text = value.toString().trim();
            if (text.length() > maxLength) {
                throw new IllegalArgumentException(key + " 长度不能超过" + maxLength);
            }
            setter.accept(text);
        }
    }
}
