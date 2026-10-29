package com.typinggame.user.service.impl;

import com.typinggame.user.entity.UserAppConfig;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.user.repository.UserAppConfigRepository;
import com.typinggame.user.service.UserRegistrationInitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

/**
 * 注册初始化：user-svc 只写应用偏好；对局/模型/怪物配置委托 game-core bootstrap。
 */
@Service
public class UserRegistrationInitServiceImpl implements UserRegistrationInitService {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationInitServiceImpl.class);

    private final UserAppConfigRepository userAppConfigRepository;
    private final RestTemplate restTemplate;
    private final String gameCoreBaseUrl;

    public UserRegistrationInitServiceImpl(UserAppConfigRepository userAppConfigRepository,
                                           RestTemplate userRestTemplate,
                                           @Value("${user.bootstrap.game-core-base-url:http://game-core-svc/game-core-svc}")
                                           String gameCoreBaseUrl) {
        this.userAppConfigRepository = userAppConfigRepository;
        this.restTemplate = userRestTemplate;
        this.gameCoreBaseUrl = gameCoreBaseUrl.endsWith("/")
                ? gameCoreBaseUrl.substring(0, gameCoreBaseUrl.length() - 1)
                : gameCoreBaseUrl;
    }

    @Override
    public void initForNewUser(UserInfo user) {
        if (user == null || user.getUserId() == null) {
            log.warn("initForNewUser: user or userId is null, skip");
            return;
        }
        Integer userId = user.getUserId();
        try {
            initUserAppConfig(userId);
        } catch (Exception e) {
            log.error("Init user_app_configs failed for userId={}", userId, e);
        }
        try {
            String url = gameCoreBaseUrl + "/api/v1/internal/bootstrap-user/" + userId;
            restTemplate.postForEntity(url, null, Void.class);
        } catch (Exception e) {
            log.error("game-core bootstrap failed for userId={}", userId, e);
        }
    }

    protected void initUserAppConfig(Integer userId) {
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
        Date now = new Date();
        config.setCreateTime(now);
        config.setUpdateTime(now);
        userAppConfigRepository.save(config);
        log.debug("Initialized user_app_configs for userId={}", userId);
    }
}
