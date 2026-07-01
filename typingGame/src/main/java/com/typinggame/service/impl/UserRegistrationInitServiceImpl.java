package com.typinggame.service.impl;

import com.typinggame.entity.config.GameCharTypeConfig;
import com.typinggame.entity.config.SystemGameModeConfig;
import com.typinggame.entity.config.UserAppConfig;
import com.typinggame.entity.game.SystemMonsterConfig;
import com.typinggame.entity.game.UserGameModeConfig;
import com.typinggame.entity.game.UserMonsterConfig;
import com.typinggame.entity.model.ModelInitParams;
import com.typinggame.entity.model.UserModelParams;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.repository.GameCharTypeConfigRepository;
import com.typinggame.repository.GameModeConfigRepository;
import com.typinggame.repository.ModelInitParamsRepository;
import com.typinggame.repository.SystemMonsterConfigRepository;
import com.typinggame.repository.UserAppConfigRepository;
import com.typinggame.repository.UserGameModeConfigRepository;
import com.typinggame.repository.UserMonsterConfigRepository;
import com.typinggame.repository.UserModelParamsRepository;
import com.typinggame.service.UserRegistrationInitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * 新用户注册后基础表初始化实现。
 * 依次初始化：user_app_configs、user_model_params、user_game_mode_configs、
 * user_monster_configs、game_char_type_configs。
 */
@Service
public class UserRegistrationInitServiceImpl implements UserRegistrationInitService {

    /** 默认勾选的字符类型（与 character_type_configs 初始数据一致） */
    private static final String DEFAULT_CHAR_TYPES = "lower,upper,number,punct,operator,chinese,special";

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationInitServiceImpl.class);

    @Autowired
    private UserAppConfigRepository userAppConfigRepository;
    @Autowired
    private UserModelParamsRepository userModelParamsRepository;
    @Autowired
    private ModelInitParamsRepository modelInitParamsRepository;
    @Autowired
    private UserGameModeConfigRepository userGameModeConfigRepository;
    @Autowired
    private GameModeConfigRepository gameModeConfigRepository;
    @Autowired
    private UserMonsterConfigRepository userMonsterConfigRepository;
    @Autowired
    private SystemMonsterConfigRepository systemMonsterConfigRepository;
    @Autowired
    private GameCharTypeConfigRepository gameCharTypeConfigRepository;

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
            initUserModelParams(userId);
        } catch (Exception e) {
            log.error("Init user_model_params failed for userId={}", userId, e);
        }

        try {
            initUserGameModeConfigs(user);
        } catch (Exception e) {
            log.error("Init user_game_mode_configs failed for userId={}", userId, e);
        }

        try {
            initUserMonsterConfigs(userId);
        } catch (Exception e) {
            log.error("Init user_monster_configs failed for userId={}", userId, e);
        }

        try {
            initGameCharTypeConfigs(userId);
        } catch (Exception e) {
            log.error("Init game_char_type_configs failed for userId={}", userId, e);
        }
    }

    /**
     * 插入一条默认用户应用配置
     */
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

    /**
     * 按系统 model_init_params 为每个 game_mode 插入一条 user_model_params
     */
    protected void initUserModelParams(Integer userId) {
        List<ModelInitParams> systemParams = modelInitParamsRepository.findAll();
        if (systemParams == null || systemParams.isEmpty()) {
            log.warn("No model_init_params found, skip init user_model_params for userId={}", userId);
            return;
        }
        for (ModelInitParams init : systemParams) {
            if (userModelParamsRepository.findByUserIdAndGameMode(userId, init.getGameMode()).isPresent()) {
                continue;
            }
            UserModelParams p = mapToUserModelParams(init, userId);
            userModelParamsRepository.save(p);
            log.debug("Initialized user_model_params for userId={}, gameMode={}", userId, init.getGameMode());
        }
    }

    /**
     * 按系统 game_mode_configs 为每个模式插入一条 user_game_mode_configs
     */
    protected void initUserGameModeConfigs(UserInfo user) {
        List<SystemGameModeConfig> systemModes = gameModeConfigRepository.findAll();
        if (systemModes == null || systemModes.isEmpty()) {
            log.warn("No game_mode_configs found, skip init user_game_mode_configs for userId={}", user.getUserId());
            return;
        }
        for (SystemGameModeConfig sys : systemModes) {
            UserGameModeConfig cfg = new UserGameModeConfig();
            cfg.setUser(user);
            cfg.setModeName(sys.getModeName());
            cfg.setMonsterInitNum(sys.getMonster_init_num() != null ? sys.getMonster_init_num() : 3);
            cfg.setMonsterTypePro(sys.getMonster_type_pro() != null ? sys.getMonster_type_pro() : "{\"NORMAL\": 0.5, \"ATTACKER\": 0.2, \"EVASIVE\": 0.15, \"SMART\": 0.15}");
            userGameModeConfigRepository.save(cfg);
            log.debug("Initialized user_game_mode_configs for userId={}, mode={}", user.getUserId(), sys.getModeName());
        }
    }

    /**
     * 按系统 system_monster_configs 为每种怪物类型插入一条 user_monster_configs
     */
    protected void initUserMonsterConfigs(Integer userId) {
        List<SystemMonsterConfig> systemMonsters = systemMonsterConfigRepository.findAll();
        if (systemMonsters == null || systemMonsters.isEmpty()) {
            log.warn("No system_monster_configs found, skip init user_monster_configs for userId={}", userId);
            return;
        }
        Date now = new Date();
        for (SystemMonsterConfig sys : systemMonsters) {
            UserMonsterConfig cfg = new UserMonsterConfig();
            cfg.setUserId(userId);
            cfg.setType(sys.getType());
            cfg.setInitSpeed(sys.getInitSpeed() != null ? BigDecimal.valueOf(sys.getInitSpeed()) : BigDecimal.ONE);
            cfg.setInitStrLength(sys.getInitStrLength() != null ? sys.getInitStrLength() : 3);
            cfg.setUpdatedAt(now);
            userMonsterConfigRepository.save(cfg);
            log.debug("Initialized user_monster_configs for userId={}, type={}", userId, sys.getType());
        }
    }

    /**
     * 按系统 game_mode_configs 为每个模式插入一条 game_char_type_configs（默认勾选全部字符类型）
     */
    protected void initGameCharTypeConfigs(Integer userId) {
        List<SystemGameModeConfig> systemModes = gameModeConfigRepository.findAll();
        if (systemModes == null || systemModes.isEmpty()) {
            log.warn("No game_mode_configs found, skip init game_char_type_configs for userId={}", userId);
            return;
        }
        Date now = new Date();
        for (SystemGameModeConfig sys : systemModes) {
            GameCharTypeConfig cfg = new GameCharTypeConfig();
            cfg.setUserId(userId);
            cfg.setGameModeId(sys.getId());
            cfg.setCharType(DEFAULT_CHAR_TYPES);
            cfg.setCreateTime(now);
            cfg.setUpdateTime(now);
            gameCharTypeConfigRepository.save(cfg);
            log.debug("Initialized game_char_type_configs for userId={}, gameModeId={}", userId, sys.getId());
        }
    }

    private UserModelParams mapToUserModelParams(ModelInitParams init, Integer userId) {
        UserModelParams p = new UserModelParams();
        p.setUserId(userId);
        p.setGameMode(init.getGameMode());
        p.setLatestRound(0);
        p.setLastDifficulty(init.getInitDifficulty());
        p.setWeightRealAccuracy(init.getWeightRealAccuracy());
        p.setWeightRealSpeed(init.getWeightRealSpeed());
        p.setWeightCharErrorRate(init.getWeightCharErrorRate());
        p.setWeightHistoryAccuracy(init.getWeightHistoryAccuracy());
        p.setWeightHistoryPeakSpeed(init.getWeightHistoryPeakSpeed());
        p.setWeightErrorCharRatio(init.getWeightErrorCharRatio());
        p.setWeightCurrentRound(init.getWeightCurrentRound());
        p.setFatigueBufferFlag(init.getFatigueBufferFlag());
        p.setProgressSpeedFlag(init.getProgressSpeedFlag());
        p.setRollbackFlag(init.getRollbackFlag());
        p.setMaxAdjustRatio(init.getMaxAdjustRatio());
        p.setSmoothAdjustCoeff(init.getSmoothAdjustCoeff());
        p.setCrossoverRate(BigDecimal.valueOf(0.8));
        p.setMutationRate(BigDecimal.valueOf(0.1));
        p.setSpeedAdjustCoeff(BigDecimal.ONE);
        p.setLengthAdjustCoeff(BigDecimal.ONE);
        p.setConsecutiveMismatchRound(init.getConsecutiveMismatchRound() != null ? init.getConsecutiveMismatchRound() : 0);
        p.setLastAdjustTime(init.getLastAdjustTime() != null ? Timestamp.valueOf(init.getLastAdjustTime()) : null);
        Date now = new Date();
        p.setCreateTime(now);
        p.setUpdateTime(now);
        return p;
    }
}
