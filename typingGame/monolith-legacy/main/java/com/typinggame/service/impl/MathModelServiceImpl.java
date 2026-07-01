package com.typinggame.service.impl;

import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.ModelInitParams;
import com.typinggame.entity.model.UserModelAdaptLog;
import com.typinggame.entity.model.UserModelParams;
import com.typinggame.repository.ModelConstantRepository;
import com.typinggame.repository.ModelInitParamsRepository;
import com.typinggame.repository.UserModelAdaptLogRepository;
import com.typinggame.repository.UserModelParamsRepository;
import com.typinggame.service.MathModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class MathModelServiceImpl implements MathModelService {

    @Autowired
    private UserModelParamsRepository userModelParamsRepository;

    @Autowired
    private UserModelAdaptLogRepository userModelAdaptLogRepository;

    @Autowired
    private ModelInitParamsRepository modelInitParamsRepository;
    @Autowired
    private ModelConstantRepository modelConstantRepository;

    @Override
    public UserModelParams getUserModelParams(Integer userId, String gameMode) {
        Optional<UserModelParams> paramsOptional = userModelParamsRepository.findByUserIdAndGameMode(userId, gameMode);
        return paramsOptional.orElse(null);
    }

    @Override
    public List<Object> getModelParamsHistory(Integer userId, String gameMode, int limit) {
        List<UserModelAdaptLog> logs = userModelAdaptLogRepository.findByUserUserIdOrderByLogTimeDesc(userId, PageRequest.of(0, limit));
        return java.util.Collections.singletonList(logs);
    }

    @Override
    public ModelConstant getModelConstant() {
        return modelConstantRepository.findAll().stream()
                .findFirst()
                .orElseGet(this::defaultModelConstant);
    }

    private ModelConstant defaultModelConstant() {
        ModelConstant c = new ModelConstant();
        c.setMaxSpeed(new BigDecimal("150"));
        c.setRefPeakSpeed(new BigDecimal("120"));
        c.setInitialDifficulty(new BigDecimal("0.30"));
        return c;
    }

    @Override
    public UserModelParams getDefaultParamsForMode(String gameMode) {
        if (gameMode == null || gameMode.trim().isEmpty()) return null;
        return modelInitParamsRepository.findFirstByGameMode(gameMode.trim())
                .map(this::toUserModelParams)
                .orElse(null);
    }

    private UserModelParams toUserModelParams(ModelInitParams init) {
        UserModelParams p = new UserModelParams();
        p.setParamId(null);
        p.setUserId(null);
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
        LocalDateTime lt = init.getLastAdjustTime();
        p.setLastAdjustTime(lt != null ? java.sql.Timestamp.valueOf(lt) : null);
        p.setCreateTime(null);
        p.setUpdateTime(null);
        return p;
    }
}

