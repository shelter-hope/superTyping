package com.typinggame.game.service.impl;

import com.typinggame.entity.config.GameCharTypeConfig;
import com.typinggame.entity.config.SystemGameModeConfig;
import com.typinggame.entity.game.SystemMonsterConfig;
import com.typinggame.game.entity.UserGameModeConfig;
import com.typinggame.game.entity.UserMonsterConfig;
import com.typinggame.entity.model.ModelInitParams;
import com.typinggame.game.entity.UserModelParams;
import com.typinggame.game.repository.GameCharTypeConfigRepository;
import com.typinggame.game.repository.GameModeConfigRepository;
import com.typinggame.game.repository.ModelInitParamsRepository;
import com.typinggame.game.repository.SystemMonsterConfigRepository;
import com.typinggame.game.repository.UserGameModeConfigRepository;
import com.typinggame.game.repository.UserModelParamsRepository;
import com.typinggame.game.repository.UserMonsterConfigRepository;
import com.typinggame.game.service.UserBootstrapService;
import com.typinggame.game.support.UserProfileClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Service
public class UserBootstrapServiceImpl implements UserBootstrapService {
    private static final String DEFAULT_CHAR_TYPES = "lower,upper,number,punct,operator,chinese,special";
    private static final Logger log = LoggerFactory.getLogger(UserBootstrapServiceImpl.class);

    private final UserProfileClient userProfileClient;
    private final UserModelParamsRepository userModelParamsRepository;
    private final ModelInitParamsRepository modelInitParamsRepository;
    private final UserGameModeConfigRepository userGameModeConfigRepository;
    private final GameModeConfigRepository gameModeConfigRepository;
    private final UserMonsterConfigRepository userMonsterConfigRepository;
    private final SystemMonsterConfigRepository systemMonsterConfigRepository;
    private final GameCharTypeConfigRepository gameCharTypeConfigRepository;

    public UserBootstrapServiceImpl(UserProfileClient userProfileClient,
                                    UserModelParamsRepository userModelParamsRepository,
                                    ModelInitParamsRepository modelInitParamsRepository,
                                    UserGameModeConfigRepository userGameModeConfigRepository,
                                    GameModeConfigRepository gameModeConfigRepository,
                                    UserMonsterConfigRepository userMonsterConfigRepository,
                                    SystemMonsterConfigRepository systemMonsterConfigRepository,
                                    GameCharTypeConfigRepository gameCharTypeConfigRepository) {
        this.userProfileClient = userProfileClient;
        this.userModelParamsRepository = userModelParamsRepository;
        this.modelInitParamsRepository = modelInitParamsRepository;
        this.userGameModeConfigRepository = userGameModeConfigRepository;
        this.gameModeConfigRepository = gameModeConfigRepository;
        this.userMonsterConfigRepository = userMonsterConfigRepository;
        this.systemMonsterConfigRepository = systemMonsterConfigRepository;
        this.gameCharTypeConfigRepository = gameCharTypeConfigRepository;
    }

    @Override
    @Transactional
    public void bootstrapUser(Integer userId) {
        if (userId == null) {
            log.warn("bootstrapUser: userId is null, skip");
            return;
        }
        if (!userProfileClient.exists(userId)) {
            log.warn("bootstrapUser: user {} not found, skip", userId);
            return;
        }
        initUserModelParams(userId);
        initUserGameModeConfigs(userId);
        initUserMonsterConfigs(userId);
        initGameCharTypeConfigs(userId);
    }

    private void initUserModelParams(Integer userId) {
        List<ModelInitParams> systemParams = modelInitParamsRepository.findAll();
        if (systemParams == null || systemParams.isEmpty()) {
            log.warn("No model_init_params found, skip for userId={}", userId);
            return;
        }
        for (ModelInitParams init : systemParams) {
            if (userModelParamsRepository.findByUserIdAndGameMode(userId, init.getGameMode()).isPresent()) {
                continue;
            }
            userModelParamsRepository.save(mapToUserModelParams(init, userId));
        }
    }

    private void initUserGameModeConfigs(Integer userId) {
        List<SystemGameModeConfig> systemModes = gameModeConfigRepository.findAll();
        if (systemModes == null || systemModes.isEmpty()) {
            log.warn("No game_mode_configs found, skip for userId={}", userId);
            return;
        }
        for (SystemGameModeConfig sys : systemModes) {
            UserGameModeConfig cfg = new UserGameModeConfig();
            cfg.setUserId(userId);
            cfg.setModeName(sys.getModeName());
            cfg.setMonsterInitNum(sys.getMonster_init_num() != null ? sys.getMonster_init_num() : 3);
            cfg.setMonsterTypePro(sys.getMonster_type_pro() != null
                    ? sys.getMonster_type_pro()
                    : "{\"NORMAL\": 0.5, \"ATTACKER\": 0.2, \"EVASIVE\": 0.15, \"SMART\": 0.15}");
            userGameModeConfigRepository.save(cfg);
        }
    }

    private void initUserMonsterConfigs(Integer userId) {
        List<SystemMonsterConfig> systemMonsters = systemMonsterConfigRepository.findAll();
        if (systemMonsters == null || systemMonsters.isEmpty()) {
            log.warn("No system_monster_configs found, skip for userId={}", userId);
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
        }
    }

    private void initGameCharTypeConfigs(Integer userId) {
        List<SystemGameModeConfig> systemModes = gameModeConfigRepository.findAll();
        if (systemModes == null || systemModes.isEmpty()) {
            return;
        }
        Date now = new Date();
        for (SystemGameModeConfig sys : systemModes) {
            if (gameCharTypeConfigRepository.findByUserIdAndGameModeId(userId, sys.getId()).isPresent()) {
                continue;
            }
            GameCharTypeConfig cfg = new GameCharTypeConfig();
            cfg.setUserId(userId);
            cfg.setGameModeId(sys.getId());
            cfg.setCharType(DEFAULT_CHAR_TYPES);
            cfg.setCreateTime(now);
            cfg.setUpdateTime(now);
            gameCharTypeConfigRepository.save(cfg);
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
