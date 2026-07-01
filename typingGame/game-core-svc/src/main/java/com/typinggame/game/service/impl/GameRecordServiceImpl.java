package com.typinggame.game.service.impl;

import com.typinggame.dto.game.GameRecordUploadDTO;
import com.typinggame.dto.game.GameRecordUploadResultDTO;
import com.typinggame.dto.game.MonsterDataDTO;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.game.MonsterData;
import com.typinggame.enums.GameMode;
import com.typinggame.game.repository.GameRecordRepository;
import com.typinggame.game.repository.MonsterDataRepository;
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
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GameRecordServiceImpl implements GameRecordService {

    private static final Logger log = LoggerFactory.getLogger(GameRecordServiceImpl.class);

    private final GameRecordRepository gameRecordRepository;
    private final MonsterDataRepository monsterDataRepository;

    @Override
    @Transactional
    public GameRecordUploadResultDTO saveRecord(Integer userId, GameRecordUploadDTO recordDTO) {
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
        record.setCreateTime(new Date());

        record = gameRecordRepository.save(record);
        Integer recordId = record.getRecordId();
        result.setRecordId(recordId);

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