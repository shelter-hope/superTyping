package com.typinggame.game.admin;

import com.typinggame.common.admin.AdminFilter;
import com.typinggame.common.admin.AdminPageSupport;
import com.typinggame.game.entity.GameRecord;
import com.typinggame.game.entity.StringParent;
import com.typinggame.game.entity.CharacterStats;
import com.typinggame.game.repository.CharacterStatsRepository;
import com.typinggame.game.repository.GameRecordRepository;
import com.typinggame.game.repository.MonsterDataRepository;
import com.typinggame.game.repository.StringParentRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RecordAdminService {
    private final GameRecordRepository gameRecordRepository;
    private final MonsterDataRepository monsterDataRepository;
    private final CharacterStatsRepository characterStatsRepository;
    private final StringParentRepository stringParentRepository;

    public RecordAdminService(GameRecordRepository gameRecordRepository,
                              MonsterDataRepository monsterDataRepository,
                              CharacterStatsRepository characterStatsRepository,
                              StringParentRepository stringParentRepository) {
        this.gameRecordRepository = gameRecordRepository;
        this.monsterDataRepository = monsterDataRepository;
        this.characterStatsRepository = characterStatsRepository;
        this.stringParentRepository = stringParentRepository;
    }

    public Map<String, Object> getGameRecords(int page, int pageSize, Integer userId, String gameMode) {
        AdminFilter<GameRecord> f = AdminFilter.of(GameRecord.class).eq("userId", userId).eq("gameMode", gameMode);
        return AdminPageSupport.pageMap("records", gameRecordRepository.findAll(f, pageable(page, pageSize, "recordId")));
    }

    @Transactional
    public boolean updateGameRecord(Integer recordId, GameRecord updates) {
        GameRecord record = gameRecordRepository.findById(recordId).orElse(null);
        if (record == null) return false;
        if (updates.getTotalScore() != null) record.setTotalScore(updates.getTotalScore());
        if (updates.getRoundNum() != null) record.setRoundNum(updates.getRoundNum());
        if (updates.getAvgSpeed() != null) record.setAvgSpeed(updates.getAvgSpeed());
        if (updates.getPeakSpeed() != null) record.setPeakSpeed(updates.getPeakSpeed());
        if (updates.getAvgAccuracy() != null) record.setAvgAccuracy(updates.getAvgAccuracy());
        if (updates.getPeakAccuracy() != null) record.setPeakAccuracy(updates.getPeakAccuracy());
        gameRecordRepository.save(record);
        return true;
    }

    @Transactional
    public void deleteGameRecord(Integer recordId) {
        monsterDataRepository.deleteByRecordId(recordId);
        if (gameRecordRepository.existsById(recordId)) {
            gameRecordRepository.deleteById(recordId);
        }
    }

    @Transactional
    public void deleteUserGameData(Integer userId) {
        monsterDataRepository.deleteByUserId(userId);
        gameRecordRepository.deleteByUserId(userId);
        characterStatsRepository.deleteByUserId(userId);
    }

    public Map<String, Object> getCharacterStats(int page, int pageSize, Integer userId, String charType, String charValue) {
        AdminFilter<CharacterStats> f = AdminFilter.of(CharacterStats.class)
                .eq("userId", userId).eq("charType", charType).eq("charValue", charValue);
        return AdminPageSupport.pageMap("items", characterStatsRepository.findAll(f, pageable(page, pageSize, "statId")));
    }

    public Map<String, Object> getMonsterData(Integer recordId, int page, int pageSize) {
        return AdminPageSupport.pageMap("items", monsterDataRepository.findByRecordId(recordId, pageable(page, pageSize, null)));
    }

    public Map<String, Object> getStringParents(int page, int pageSize, Integer userId) {
        AdminFilter<StringParent> f = AdminFilter.of(StringParent.class).eq("userId", userId);
        return AdminPageSupport.pageMap("items", stringParentRepository.findAll(f, pageable(page, pageSize, "id")));
    }

    public long countRecords() {
        return gameRecordRepository.count();
    }

    public long countRecordsSince(java.util.Date since) {
        return gameRecordRepository.count(AdminFilter.of(GameRecord.class).between("createTime", since, null));
    }

    public long countDistinctUsersSince(java.util.Date since) {
        return gameRecordRepository.countDistinctUsersSince(since);
    }

    private PageRequest pageable(int page, int pageSize, String sortDescField) {
        int p = Math.max(0, page - 1);
        int size = Math.min(200, Math.max(1, pageSize));
        return sortDescField == null ? PageRequest.of(p, size) : PageRequest.of(p, size, Sort.by(Sort.Direction.DESC, sortDescField));
    }
}
