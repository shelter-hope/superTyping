package com.typinggame.service.impl;

import com.typinggame.dto.game.GameAdjustParam;
import com.typinggame.dto.game.GamePlayerPerformanceDTO;
import com.typinggame.dto.game.GameRecordUploadDTO;
import com.typinggame.dto.game.GameRecordUploadResultDTO;
import com.typinggame.dto.game.MonsterDataDTO;
import com.typinggame.dto.stats.CharacterStatsBatchUpdateDTO;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.game.MonsterData;
import com.typinggame.entity.game.UserMonsterConfig;
import com.typinggame.entity.leaderboard.LeaderboardEntry;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.UserModelAdaptLog;
import com.typinggame.entity.model.UserModelParams;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.enums.GameMode;
import com.typinggame.repository.GameRecordRepository;
import com.typinggame.repository.LeaderboardEntryRepository;
import com.typinggame.repository.MonsterDataRepository;
import com.typinggame.repository.UserInfoRepository;
import com.typinggame.repository.UserModelAdaptLogRepository;
import com.typinggame.repository.UserModelParamsRepository;
import com.typinggame.repository.UserMonsterConfigRepository;
import com.typinggame.service.CharacterStatsService;
import com.typinggame.service.DifficultyModelService;
import com.typinggame.service.EvolutionService;
import com.typinggame.service.GameRecordService;
import com.typinggame.service.MathModelService;
import com.typinggame.util.GameCoreUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GameRecordServiceImpl implements GameRecordService {

    private static final Logger log = LoggerFactory.getLogger(GameRecordServiceImpl.class);

    private final GameRecordRepository gameRecordRepository;
    private final MonsterDataRepository monsterDataRepository;
    private final UserInfoRepository userInfoRepository;
    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final CharacterStatsService characterStatsService;
    private final EvolutionService evolutionService;

    @Autowired
    private DifficultyModelService difficultyModelService;
    @Autowired
    private MathModelService mathModelService;
    @Autowired
    private UserModelParamsRepository userModelParamsRepository;
    @Autowired
    private UserModelAdaptLogRepository userModelAdaptLogRepository;
    @Autowired
    private UserMonsterConfigRepository userMonsterConfigRepository;

    /**
     * 保存游戏记录并更新难度参数
     * @param userId 用户ID
     * @param recordDTO 游戏记录数据
     * @return 结算摘要（含 recordId、本局结算后难度 Dₜ 等）
     */
    @Override
    @Transactional
    public GameRecordUploadResultDTO saveRecord(Integer userId, GameRecordUploadDTO recordDTO) {
        GameRecordUploadResultDTO result = new GameRecordUploadResultDTO();
        result.setRecordId(null);
        result.setCurrentDifficulty(null);
        // 1. Save GameRecord
        GameRecord record = new GameRecord();
        record.setUserId(userId);
        // 前端上传的 gameMode 可能是 "endless" / "dictionary" 等，需要统一映射为后端/数据库的枚举名
        GameMode modeEnum = GameMode.fromClientMode(recordDTO.getGameMode());
        record.setGameMode(modeEnum.name());
        record.setSelectedChars(recordDTO.getSelectedChars());
        record.setRoundNum(recordDTO.getRoundNum());
        record.setTotalScore(recordDTO.getTotalScore());
        record.setTotalChars(recordDTO.getTotalChars());
        record.setCorrectChars(recordDTO.getCorrectChars());
        record.setAvgSpeed(recordDTO.getAvgSpeed());
        record.setPeakSpeed(recordDTO.getPeakSpeed());
        record.setAvgAccuracy(recordDTO.getAvgAccuracy());
        record.setPeakAccuracy(recordDTO.getPeakAccuracy());
        record.setGameDuration(recordDTO.getGameDuration());
        record.setCreateTime(new Date());
        
        record = gameRecordRepository.save(record);
        Integer recordId = record.getRecordId();
        result.setRecordId(recordId);

        // 2. Save MonsterData，并在无尽适应模式下选取父本并保存到 string_parents
        if (recordDTO.getMonsters() != null && !recordDTO.getMonsters().isEmpty()) {
            List<MonsterData> monsters = new ArrayList<>();
            for (MonsterDataDTO mdDTO : recordDTO.getMonsters()) {
                MonsterData md = new MonsterData();
                md.setRecordId(recordId);
                md.setMonsterType(mdDTO.getMonsterType());
                md.setTargetStr(mdDTO.getTargetStr());
                md.setSpeed(mdDTO.getSpeed());
                md.setSurvivalTime(mdDTO.getSurvivalTime());
                md.setDestroyCostTime(mdDTO.getDestroyCostTime());
                md.setTotalCount(mdDTO.getTotalCount());
                md.setInputErrorCount(mdDTO.getInputErrorCount());
                md.setIsTrueMean(mdDTO.getIsTrueMean());
                md.setScore(mdDTO.getScore());
                md.setDifficultyLevel(mdDTO.getDifficultyLevel());
                md.setSpawnWave(mdDTO.getSpawnWave());
                md.setIsParent(mdDTO.getIsParent());
                monsters.add(md);
            }
            monsterDataRepository.saveAll(monsters);

            // 仅在无尽适应模式下，基于本轮怪物表现选择父本并持久化，供下一轮生成使用
            if (modeEnum == GameMode.ENDLESS_ADAPT) {
                List<MonsterData> parents = evolutionService.selectParents(userId, monsters);
                if (parents != null && !parents.isEmpty()) {
                    evolutionService.saveParentStrings(userId, parents);
                }
            }

            // 7.4 可选：按本局各类型怪物实际速度/长度平滑更新 user_monster_configs
            smoothUpdateUserMonsterConfigs(userId, recordDTO.getMonsters());
        }
        
        // 3. Update UserInfo
        UserInfo user = userInfoRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setGameNum((user.getGameNum() == null ? 0 : user.getGameNum()) + 1);
            
            if (user.getBestScore() == null || (recordDTO.getTotalScore() != null && recordDTO.getTotalScore() > user.getBestScore())) {
                user.setBestScore(recordDTO.getTotalScore());
            }
            if (user.getBestRound() == null || (recordDTO.getRoundNum() != null && recordDTO.getRoundNum() > user.getBestRound())) {
                user.setBestRound(recordDTO.getRoundNum());
            }
            if (user.getBestSpeed() == null || (recordDTO.getPeakSpeed() != null && recordDTO.getPeakSpeed().compareTo(user.getBestSpeed()) > 0)) {
                user.setBestSpeed(recordDTO.getPeakSpeed());
            }
            if (user.getBestAccuracy() == null || (recordDTO.getPeakAccuracy() != null && recordDTO.getPeakAccuracy().compareTo(user.getBestAccuracy()) > 0)) {
                user.setBestAccuracy(recordDTO.getPeakAccuracy());
            }
            
            // Recalculate Averages
            int n = user.getGameNum();
            if (n > 1) {
                 if (user.getAvgSpeed() != null && recordDTO.getAvgSpeed() != null) {
                     BigDecimal totalSpeed = user.getAvgSpeed().multiply(new BigDecimal(n - 1)).add(recordDTO.getAvgSpeed());
                     user.setAvgSpeed(totalSpeed.divide(new BigDecimal(n), 2, BigDecimal.ROUND_HALF_UP));
                 }
                 if (user.getAvgAccuracy() != null && recordDTO.getAvgAccuracy() != null) {
                     BigDecimal totalAcc = user.getAvgAccuracy().multiply(new BigDecimal(n - 1)).add(recordDTO.getAvgAccuracy());
                     user.setAvgAccuracy(totalAcc.divide(new BigDecimal(n), 2, BigDecimal.ROUND_HALF_UP));
                 }
            } else {
                 if (recordDTO.getAvgSpeed() != null) user.setAvgSpeed(recordDTO.getAvgSpeed());
                 if (recordDTO.getAvgAccuracy() != null) user.setAvgAccuracy(recordDTO.getAvgAccuracy());
            }
            
            user.setUpdateTime(new Date());
            userInfoRepository.save(user);
        }
        
        // 4. Update Leaderboard
        LeaderboardEntry lbEntry = new LeaderboardEntry();
        lbEntry.setRecordId(recordId);
        lbEntry.setUserId(userId);
        if (user != null) {
            lbEntry.setUsername(user.getUsername());
            lbEntry.setNickname(user.getNickname());
            lbEntry.setAvatar(user.getAvatar());
        }
        lbEntry.setGameMode(modeEnum.name());
        lbEntry.setTotalScore(recordDTO.getTotalScore());
        lbEntry.setRoundNum(recordDTO.getRoundNum());
        lbEntry.setAvgSpeed(recordDTO.getAvgSpeed());
        lbEntry.setPeakSpeed(recordDTO.getPeakSpeed());
        lbEntry.setAvgAccuracy(recordDTO.getAvgAccuracy());
        lbEntry.setPeakAccuracy(recordDTO.getPeakAccuracy());
        lbEntry.setGameDuration(recordDTO.getGameDuration());
        lbEntry.setCreateTime(new Date());
        lbEntry.setRank(0); // 暂定为0

        leaderboardEntryRepository.save(lbEntry);

        // 5. 更新 character_stats：本局每个字符的输入结果，供个人页「字符统计（错误最多）」展示
        if (recordDTO.getCharacterStatsUpdates() != null && !recordDTO.getCharacterStatsUpdates().isEmpty()) {
            log.info("Character stats updates: userId={} count={}", userId, recordDTO.getCharacterStatsUpdates().size());
            CharacterStatsBatchUpdateDTO batchDTO = new CharacterStatsBatchUpdateDTO();
            batchDTO.setUpdates(recordDTO.getCharacterStatsUpdates());
            characterStatsService.batchUpdate(userId, batchDTO);
        }

        // 6. 更新用户个性化难度参数并记录适配日志（无尽适应、词源界定、限时冲刺共用同一套 Dₜ 模型）
        if (modeEnum == GameMode.ENDLESS_ADAPT || modeEnum == GameMode.WORD_SOURCE || modeEnum == GameMode.LIMIT_TIME) {
            try {
                GamePlayerPerformanceDTO aggregated =
                        difficultyModelService.buildAggregatedPerformance(userId, modeEnum.name());

                GamePlayerPerformanceDTO perf = new GamePlayerPerformanceDTO();
                perf.setRealAccuracy(record.getAvgAccuracy());
                perf.setRealSpeed(record.getAvgSpeed());
                perf.setCurrentRound(record.getRoundNum());
                if (aggregated != null) {
                    perf.setHistoryAvgAccuracy(aggregated.getHistoryAvgAccuracy());
                    perf.setHistoryPeakSpeed(aggregated.getHistoryPeakSpeed());
                    perf.setAvgCharErrorRate(aggregated.getAvgCharErrorRate());
                    perf.setErrorCharRatio(aggregated.getErrorCharRatio());
                }

                UserModelParams params = mathModelService.getUserModelParams(userId, modeEnum.name());
                if (params != null) {
                    ModelConstant constant = mathModelService.getModelConstant();

                    BigDecimal beforeRaw = params.getLastDifficulty() != null
                            ? params.getLastDifficulty()
                            : constant.getInitialDifficulty();

                    GameAdjustParam adjustParam = GameCoreUtil.updateGameParam(
                            null,
                            record,
                            constant,
                            params,
                            perf.getErrorCharRatio());

                    if (adjustParam != null && adjustParam.getCurrentDifficulty() != null) {
                        // 难度系数按 [0,9.99] 约束并与表结构 DECIMAL(3,2) 对齐
                        BigDecimal min = BigDecimal.ZERO;
                        BigDecimal max = new BigDecimal("9.99");

                        BigDecimal beforeBounded = beforeRaw;
                        log.info("beforeBounded: {}", beforeBounded);
                        if (beforeBounded.compareTo(min) < 0) beforeBounded = min;
                        if (beforeBounded.compareTo(max) > 0) beforeBounded = max;
                        BigDecimal beforeDifficulty = beforeBounded.setScale(2, BigDecimal.ROUND_HALF_UP);

                        BigDecimal afterRaw = adjustParam.getCurrentDifficulty();
                        BigDecimal afterBounded = afterRaw;
                        if (afterBounded.compareTo(min) < 0) afterBounded = min;
                        if (afterBounded.compareTo(max) > 0) afterBounded = max;
                        BigDecimal afterDifficulty = afterBounded.setScale(2, BigDecimal.ROUND_HALF_UP);

                        result.setCurrentDifficulty(afterDifficulty);

                        params.setLastDifficulty(afterDifficulty);
                        params.setLatestRound(record.getRoundNum());
                        params.setLastAdjustTime(new Date());
                        params.setUpdateTime(new Date());
                        userModelParamsRepository.save(params);

                        UserModelAdaptLog log = new UserModelAdaptLog();
                        log.setUser(user);
                        log.setGameRecord(record);
                        log.setRoundNum(record.getRoundNum());
                        log.setBeforeDifficulty(beforeDifficulty);
                        log.setAfterDifficulty(afterDifficulty);
                        log.setAdjustParams(buildAdjustParamsJson(adjustParam));
                        log.setTriggerRule(null);
                        userModelAdaptLogRepository.save(log);
                    }
                }
            } catch (Exception e) {
                // 不影响游戏记录主流程，但需要打日志方便排查
                log.error("Update user_model_params failed for userId={}, mode={}", userId, modeEnum.name(), e);
            }
        }

        return result;
    }

    @Override
    public List<GameRecord> getRecords(Integer userId, String mode, Integer limit, Date startDate, Date endDate) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createTime"));
        
        if (startDate != null && endDate != null) {
            return gameRecordRepository.findByUserIdAndCreateTimeBetween(userId, startDate, endDate, pageRequest);
        } else if (startDate != null) {
            return gameRecordRepository.findByUserIdAndCreateTimeAfter(userId, startDate, pageRequest);
        }

        // mode 为前端传入字符串（如 "endless"），需要先转换为后端标准枚举名
        if (mode != null) {
            String dbMode = GameMode.fromClientMode(mode).name();
            return gameRecordRepository.findByUserIdAndGameMode(userId, dbMode, pageRequest);
        } else {
            return gameRecordRepository.findByUserId(userId, pageRequest);
        }
    }

    @Override
    public Page<GameRecord> getAllRecords(Pageable pageable, String searchTerm, String gameMode) {
        // searchTerm assumed to be userId for now as GameRecord only has userId
        Integer userId = null;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            try {
                userId = Integer.parseInt(searchTerm);
            } catch (NumberFormatException e) {
                // ignore or return empty
                // if search term is not a number, we can't search by userId
                // Maybe search by username requires join, which is complex.
                // For now, assume userId search or return empty if invalid format
                // Or maybe we just ignore search if not number
            }
        }

        if (userId != null) {
            if (gameMode != null && !gameMode.isEmpty()) {
                String dbMode = GameMode.fromClientMode(gameMode).name();
                return gameRecordRepository.findAllByUserIdAndGameMode(userId, dbMode, pageable);
            } else {
                return gameRecordRepository.findAllByUserId(userId, pageable);
            }
        } else {
            if (gameMode != null && !gameMode.isEmpty()) {
                String dbMode = GameMode.fromClientMode(gameMode).name();
                return gameRecordRepository.findByGameMode(dbMode, pageable);
            } else {
                return gameRecordRepository.findAll(pageable);
            }
        }
    }

    @Override
    public boolean updateRecord(Integer recordId, GameRecord updates) {
        return gameRecordRepository.findById(recordId).map(record -> {
            if (updates.getTotalScore() != null) record.setTotalScore(updates.getTotalScore());
            if (updates.getRoundNum() != null) record.setRoundNum(updates.getRoundNum());
            if (updates.getAvgSpeed() != null) record.setAvgSpeed(updates.getAvgSpeed());
            if (updates.getPeakSpeed() != null) record.setPeakSpeed(updates.getPeakSpeed());
            if (updates.getAvgAccuracy() != null) record.setAvgAccuracy(updates.getAvgAccuracy());
            if (updates.getPeakAccuracy() != null) record.setPeakAccuracy(updates.getPeakAccuracy());
            gameRecordRepository.save(record);
            return true;
        }).orElse(false);
    }

    @Override
    public void deleteRecord(Integer recordId) {
        gameRecordRepository.deleteById(recordId);
    }

    @Override
    public GameRecord getRecordDetail(Long id) {
        return gameRecordRepository.findById(id.intValue()).orElse(null);
    }

    @Override
    public GameRecord getBestRecord(Integer userId, String mode) {
        String dbMode = mode != null ? GameMode.fromClientMode(mode).name() : GameMode.ENDLESS_ADAPT.name();
        return gameRecordRepository.findTopByUserIdAndGameModeOrderByTotalScoreDesc(userId, dbMode);
    }

    private String buildAdjustParamsJson(GameAdjustParam param) {
        StringBuilder sb = new StringBuilder("{");
        if (param.getMonsterSpeed() != null) {
            sb.append("\"monsterSpeed\":").append(param.getMonsterSpeed());
        }
        if (param.getMonsterCount() != null) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"monsterCount\":").append(param.getMonsterCount());
        }
        if (param.getAvgStringLength() != null) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"avgStringLength\":").append(param.getAvgStringLength());
        }
        if (param.getNormalRatio() != null) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"normalRatio\":").append(param.getNormalRatio());
        }
        if (param.getAttackRatio() != null) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"attackRatio\":").append(param.getAttackRatio());
        }
        if (param.getEvasiveRatio() != null) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"evasiveRatio\":").append(param.getEvasiveRatio());
        }
        if (param.getSmartRatio() != null) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"smartRatio\":").append(param.getSmartRatio());
        }
        sb.append("}");
        return sb.toString();
    }

    /**  可选：按本局各类型怪物实际速度/长度平滑更新 user_monster_configs（α=0.8 保留旧值） */
    private void smoothUpdateUserMonsterConfigs(Integer userId, List<MonsterDataDTO> monsters) {
        if (monsters == null || monsters.isEmpty()) return;
        try {
            Map<String, List<MonsterDataDTO>> byType = monsters.stream()
                    .filter(m -> m.getMonsterType() != null && !m.getMonsterType().isEmpty())
                    .collect(Collectors.groupingBy(MonsterDataDTO::getMonsterType));
            BigDecimal alpha = new BigDecimal("0.8");
            BigDecimal oneMinusAlpha = new BigDecimal("0.2");
            Date now = new Date();
            for (Map.Entry<String, List<MonsterDataDTO>> e : byType.entrySet()) {
                String type = e.getKey();
                List<MonsterDataDTO> list = e.getValue();
                if (list.isEmpty()) continue;
                BigDecimal sumSpeed = list.stream()
                        .map(m -> m.getSpeed() != null ? m.getSpeed() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                int sumLen = list.stream()
                        .mapToInt(m -> m.getTargetStr() != null ? m.getTargetStr().length() : 0)
                        .sum();
                BigDecimal sessionAvgSpeed = sumSpeed.divide(BigDecimal.valueOf(list.size()), 4, RoundingMode.HALF_UP);
                int sessionAvgLen = sumLen / list.size();
                userMonsterConfigRepository.findByUserIdAndType(userId, type).ifPresent(uc -> {
                    if (uc.getInitSpeed() != null) {
                        uc.setInitSpeed(uc.getInitSpeed().multiply(alpha).add(sessionAvgSpeed.multiply(oneMinusAlpha)).setScale(4, RoundingMode.HALF_UP));
                    } else {
                        uc.setInitSpeed(sessionAvgSpeed);
                    }
                    if (uc.getInitStrLength() != null) {
                        uc.setInitStrLength((int) (uc.getInitStrLength() * 0.8 + sessionAvgLen * 0.2));
                    } else {
                        uc.setInitStrLength(Math.max(1, sessionAvgLen));
                    }
                    uc.setUpdatedAt(now);
                    userMonsterConfigRepository.save(uc);
                });
            }
        } catch (Exception e) {
            log.debug("Smooth update user_monster_configs skipped: {}", e.getMessage());
        }
    }
}
