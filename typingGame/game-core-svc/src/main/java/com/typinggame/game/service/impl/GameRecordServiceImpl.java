package com.typinggame.game.service.impl;

import com.typinggame.common.exception.BusinessException;
import com.typinggame.dto.game.GameRecordUploadDTO;
import com.typinggame.dto.game.GameRecordUploadResultDTO;
import com.typinggame.dto.game.MonsterDataDTO;
import com.typinggame.dto.stats.CharacterStatsBatchUpdateDTO;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.game.MonsterData;
import com.typinggame.entity.leaderboard.LeaderboardEntry;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.enums.GameMode;
import com.typinggame.game.repository.GameLeaderboardEntryRepository;
import com.typinggame.game.repository.GameRecordRepository;
import com.typinggame.game.repository.GameUserInfoRepository;
import com.typinggame.game.repository.MonsterDataRepository;
import com.typinggame.game.service.CharacterStatsService;
import com.typinggame.game.service.GameRecordPostProcessService;
import com.typinggame.game.service.GameRecordService;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GameRecordServiceImpl implements GameRecordService {

    private static final int MAX_ROUND = 10000;
    private static final int MAX_TOTAL_CHARS = 1_000_000;
    private static final int MAX_MONSTERS = 10000;
    private static final int MAX_CHARACTER_UPDATES = 10000;

    private static final Logger log = LoggerFactory.getLogger(GameRecordServiceImpl.class);

    private final GameRecordRepository gameRecordRepository;
    private final MonsterDataRepository monsterDataRepository;
    private final GameLeaderboardEntryRepository leaderboardEntryRepository;
    private final GameUserInfoRepository userInfoRepository;
    private final GameRecordPostProcessService postProcessService;
    private final CharacterStatsService characterStatsService;

    @Override
    @Transactional
    public GameRecordUploadResultDTO saveRecord(Integer userId, GameRecordUploadDTO recordDTO) {
        validateUpload(recordDTO);
        GameRecordUploadResultDTO result = new GameRecordUploadResultDTO();
        result.setRecordId(null);
        result.setCurrentDifficulty(null);

        GameRecord record = new GameRecord();
        record.setUserId(userId);
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
        record.setSessionId(recordDTO.getSessionId());
        record.setLastRoundId(recordDTO.getLastRoundId());
        record.setAccuracyRatio(resolveAccuracyRatio(recordDTO));
        record.setGameDurationMs(resolveDurationMillis(recordDTO));
        record.setDataVersion(recordDTO.getDataVersion() == null ? 1 : recordDTO.getDataVersion());
        record.setCreateTime(new Date());

        record = gameRecordRepository.save(record);
        Integer recordId = record.getRecordId();
        result.setRecordId(recordId);

        List<MonsterData> monsters = saveMonsters(recordId, recordDTO.getMonsters());

        if (recordDTO.getCharacterStatsUpdates() != null && !recordDTO.getCharacterStatsUpdates().isEmpty()) {
            CharacterStatsBatchUpdateDTO batch = new CharacterStatsBatchUpdateDTO();
            batch.setUpdates(recordDTO.getCharacterStatsUpdates());
            characterStatsService.batchUpdate(userId, batch);
        }

        syncLeaderboardAndUserSummary(record);
        result.setCurrentDifficulty(postProcessService.process(userId, record, recordDTO, monsters));

        return result;
    }

    private void validateUpload(GameRecordUploadDTO dto) {
        if (dto == null) {
            throw BusinessException.badRequest("游戏记录不能为空");
        }
        validateNonNegative(dto.getRoundNum(), "轮次", MAX_ROUND);
        validateNonNegative(dto.getTotalScore(), "总分", Integer.MAX_VALUE);
        validateNonNegative(dto.getTotalChars(), "总字符数", MAX_TOTAL_CHARS);
        validateNonNegative(dto.getCorrectChars(), "正确字符数", MAX_TOTAL_CHARS);
        if (dto.getTotalChars() != null && dto.getCorrectChars() != null
                && dto.getCorrectChars() > dto.getTotalChars()) {
            throw BusinessException.badRequest("正确字符数不能大于总字符数");
        }
        validateNonNegative(dto.getGameDuration(), "游戏时长", 24 * 60 * 60);
        validateDecimal(dto.getAvgSpeed(), "平均速度", 10000);
        validateDecimal(dto.getPeakSpeed(), "峰值速度", 10000);
        validateDecimal(dto.getAvgAccuracy(), "平均准确率", 100);
        validateDecimal(dto.getPeakAccuracy(), "峰值准确率", 100);

        if (dto.getMonsters() != null && dto.getMonsters().size() > MAX_MONSTERS) {
            throw BusinessException.badRequest("怪物数据数量不能超过" + MAX_MONSTERS);
        }
        if (dto.getMonsters() != null) {
            dto.getMonsters().forEach(monster -> {
                if (monster == null) {
                    throw BusinessException.badRequest("怪物数据不能为空");
                }
                validateDecimal(monster.getSpeed(), "怪物速度", 10000);
                validateNonNegative(monster.getSurvivalTime(), "怪物存活时间", 24 * 60 * 60);
                validateNonNegative(monster.getDestroyCostTime(), "怪物击杀耗时", 24 * 60 * 60);
                validateNonNegative(monster.getTotalCount(), "怪物输入总数", MAX_TOTAL_CHARS);
                validateNonNegative(monster.getInputErrorCount(), "怪物输入错误数", MAX_TOTAL_CHARS);
                validateNonNegative(monster.getScore(), "怪物得分", Integer.MAX_VALUE);
                validateNonNegative(monster.getDifficultyLevel(), "怪物难度", 100);
                validateNonNegative(monster.getSpawnWave(), "怪物波次", MAX_ROUND);
                if (monster.getTotalCount() != null && monster.getInputErrorCount() != null
                        && monster.getInputErrorCount() > monster.getTotalCount()) {
                    throw BusinessException.badRequest("怪物输入错误数不能大于输入总数");
                }
                if (monster.getTargetStr() != null && monster.getTargetStr().length() > 200) {
                    throw BusinessException.badRequest("怪物目标文本过长");
                }
            });
        }
        if (dto.getCharacterStatsUpdates() != null
                && dto.getCharacterStatsUpdates().size() > MAX_CHARACTER_UPDATES) {
            throw BusinessException.badRequest("字符统计数据数量不能超过" + MAX_CHARACTER_UPDATES);
        }
    }

    private void validateNonNegative(Integer value, String field, int max) {
        if (value != null && (value < 0 || value > max)) {
            throw BusinessException.badRequest(field + "超出允许范围");
        }
    }

    private void validateDecimal(BigDecimal value, String field, int max) {
        if (value != null && (value.signum() < 0 || value.compareTo(BigDecimal.valueOf(max)) > 0)) {
            throw BusinessException.badRequest(field + "超出允许范围");
        }
    }

    private List<MonsterData> saveMonsters(Integer recordId, List<MonsterDataDTO> monsterDTOs) {
        if (monsterDTOs == null || monsterDTOs.isEmpty()) {
            return List.of();
        }
        List<MonsterData> monsters = new ArrayList<>();
        for (MonsterDataDTO mdDTO : monsterDTOs) {
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
            md.setIsParent(Boolean.TRUE.equals(mdDTO.getIsParent()));
            md.setRoundId(mdDTO.getRoundId());
            md.setInputCount(mdDTO.getInputCount() == null ? mdDTO.getTotalCount() : mdDTO.getInputCount());
            md.setCorrectCount(mdDTO.getCorrectCount() == null
                    ? Math.max(0, safeInt(md.getInputCount()) - safeInt(mdDTO.getInputErrorCount()))
                    : mdDTO.getCorrectCount());
            md.setErrorCount(mdDTO.getErrorCount() == null ? mdDTO.getInputErrorCount() : mdDTO.getErrorCount());
            md.setResult(mdDTO.getResult());
            md.setGeneration(mdDTO.getGeneration());
            md.setPlanVersion(mdDTO.getPlanVersion());
            monsters.add(md);
        }
        return monsterDataRepository.saveAll(monsters);
    }

    private BigDecimal resolveAccuracyRatio(GameRecordUploadDTO dto) {
        if (dto.getAccuracyRatio() != null) {
            return dto.getAccuracyRatio();
        }
        if (dto.getAvgAccuracy() == null) {
            return null;
        }
        return dto.getAvgAccuracy().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
    }

    private Long resolveDurationMillis(GameRecordUploadDTO dto) {
        if (dto.getGameDurationMs() != null) {
            return dto.getGameDurationMs();
        }
        return dto.getGameDuration() == null ? null : dto.getGameDuration() * 1000L;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private void syncLeaderboardAndUserSummary(GameRecord record) {
        UserInfo user = userInfoRepository.findById(record.getUserId()).orElse(null);
        if (user == null) {
            return;
        }
        Integer score = record.getTotalScore() == null ? 0 : record.getTotalScore();
        LeaderboardEntry entry = leaderboardEntryRepository.findByUserIdAndGameMode(record.getUserId(), record.getGameMode())
                .stream()
                .max(Comparator.comparing(LeaderboardEntry::getTotalScore,
                                Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(LeaderboardEntry::getCreateTime,
                                Comparator.nullsFirst(Date::compareTo))
                        .thenComparing(LeaderboardEntry::getEntryId,
                                Comparator.nullsFirst(Integer::compareTo)))
                .orElseGet(LeaderboardEntry::new);
        boolean shouldUpdateEntry = entry.getEntryId() == null || entry.getTotalScore() == null || score >= entry.getTotalScore();
        if (shouldUpdateEntry) {
            entry.setRecordId(record.getRecordId());
            entry.setUserId(record.getUserId());
            entry.setUsername(user.getUsername());
            entry.setNickname(user.getNickname());
            entry.setAvatar(user.getAvatar());
            entry.setGameMode(record.getGameMode());
            entry.setTotalScore(score);
            entry.setRoundNum(record.getRoundNum());
            entry.setAvgSpeed(record.getAvgSpeed());
            entry.setPeakSpeed(record.getPeakSpeed());
            entry.setAvgAccuracy(record.getAvgAccuracy());
            entry.setPeakAccuracy(record.getPeakAccuracy());
            entry.setGameDuration(record.getGameDuration());
            entry.setCreateTime(record.getCreateTime());
            leaderboardEntryRepository.save(entry);
        }

        if (user.getBestScore() == null || score > user.getBestScore()) {
            user.setBestScore(score);
            user.setBestRound(record.getRoundNum());
            user.setBestSpeed(record.getPeakSpeed());
            user.setBestAccuracy(record.getPeakAccuracy());
        }
        user.setGameNum((user.getGameNum() == null ? 0 : user.getGameNum()) + 1);
        user.setAvgSpeed(record.getAvgSpeed());
        user.setAvgAccuracy(record.getAvgAccuracy());
        user.setPeakAccuracy(record.getPeakAccuracy());
        user.setUpdateTime(new Date());
        userInfoRepository.save(user);
    }

    @Override
    public List<GameRecord> getRecords(Integer userId, String mode, Integer limit, Date startDate, Date endDate) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createTime"));

        if (startDate != null && endDate != null) {
            return gameRecordRepository.findByUserIdAndCreateTimeBetween(userId, startDate, endDate, pageRequest);
        } else if (startDate != null) {
            return gameRecordRepository.findByUserIdAndCreateTimeAfter(userId, startDate, pageRequest);
        }

        if (mode != null) {
            String dbMode = GameMode.fromClientMode(mode).name();
            return gameRecordRepository.findByUserIdAndGameMode(userId, dbMode, pageRequest);
        } else {
            return gameRecordRepository.findByUserId(userId, pageRequest);
        }
    }

    @Override
    public Page<GameRecord> getAllRecords(Pageable pageable, String searchTerm, String gameMode) {
        Integer userId = null;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            try {
                userId = Integer.parseInt(searchTerm);
            } catch (NumberFormatException e) {
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
}
