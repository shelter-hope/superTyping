package com.typinggame.game.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.common.exception.BusinessException;
import com.typinggame.entity.config.SystemParam;
import com.typinggame.entity.game.SystemMonsterConfig;
import com.typinggame.game.admin.ConfigAdminService;
import com.typinggame.game.admin.LeaderboardAdminService;
import com.typinggame.game.admin.RecordAdminService;
import com.typinggame.game.admin.RuleAdminService;
import com.typinggame.game.repository.CharacterStatsRepository;
import com.typinggame.game.repository.ConfigTemplateRepository;
import com.typinggame.game.repository.GameLeaderboardEntryRepository;
import com.typinggame.game.repository.GameModeConfigRepository;
import com.typinggame.game.repository.GameRecordRepository;
import com.typinggame.game.repository.ModelConstantRepository;
import com.typinggame.game.repository.ModelInitParamsRepository;
import com.typinggame.game.repository.MonsterDataRepository;
import com.typinggame.game.repository.StringParentRepository;
import com.typinggame.game.repository.SystemMonsterCommoConfigRepository;
import com.typinggame.game.repository.SystemMonsterConfigRepository;
import com.typinggame.game.repository.SystemMonsterTypeConfigRepository;
import com.typinggame.game.repository.SystemParamRepository;
import com.typinggame.game.repository.UserModelAdaptLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalAdminServiceImplTest {

    private static final String MOVEMENT = "admin.movement_rules";

    private SystemParamRepository systemParamRepository;
    private SystemMonsterConfigRepository monsterConfigRepository;
    private InternalAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        systemParamRepository = mock(SystemParamRepository.class);
        monsterConfigRepository = mock(SystemMonsterConfigRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        RecordAdminService records = new RecordAdminService(
                mock(GameRecordRepository.class), mock(MonsterDataRepository.class),
                mock(CharacterStatsRepository.class), mock(StringParentRepository.class));
        ConfigAdminService configs = new ConfigAdminService(
                systemParamRepository, mock(SystemMonsterTypeConfigRepository.class),
                mock(SystemMonsterCommoConfigRepository.class), monsterConfigRepository,
                mock(ModelConstantRepository.class), mock(ModelInitParamsRepository.class),
                mock(GameModeConfigRepository.class), mock(UserModelAdaptLogRepository.class), objectMapper);
        RuleAdminService rules = new RuleAdminService(systemParamRepository, mock(ConfigTemplateRepository.class), objectMapper);
        LeaderboardAdminService leaderboard = new LeaderboardAdminService(mock(GameLeaderboardEntryRepository.class), records);
        service = new InternalAdminServiceImpl(records, configs, rules, leaderboard);
    }

    @Test
    void returnsDefaultsWhenRuleNotStored() {
        when(systemParamRepository.findByParamName(MOVEMENT)).thenReturn(Optional.empty());

        Map<String, Object> rules = service.getRules("movement-rules");

        assertEquals(0.8, rules.get("minSpeed"));
        assertEquals(3.0, rules.get("maxSpeed"));
    }

    @Test
    void savedRulesArePersistedAsJsonAndReadBack() {
        when(systemParamRepository.findByParamName(MOVEMENT)).thenReturn(Optional.empty());

        service.saveRules("movement-rules", Map.of("minSpeed", 1.5, "maxSpeed", 2.5));

        ArgumentCaptor<SystemParam> saved = ArgumentCaptor.forClass(SystemParam.class);
        verify(systemParamRepository).save(saved.capture());
        assertEquals(MOVEMENT, saved.getValue().getParamName());
        assertEquals("json", saved.getValue().getParamType());

        when(systemParamRepository.findByParamName(MOVEMENT)).thenReturn(Optional.of(saved.getValue()));
        assertEquals(1.5, service.getRules("movement-rules").get("minSpeed"));
    }

    @Test
    void rejectsInvalidMovementRules() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveRules("movement-rules", Map.of("minSpeed", 3, "maxSpeed", 1)));
        assertEquals(400, error.getCode());
        verify(systemParamRepository, never()).save(any());
    }

    @Test
    void updatesMonsterConfigFields() {
        SystemMonsterConfig config = new SystemMonsterConfig();
        config.setType("NORMAL");
        when(monsterConfigRepository.findByType("NORMAL")).thenReturn(Optional.of(config));

        service.updateMonsterConfig("NORMAL", Map.of("score", 12, "initSpeed", 1.2, "initStrLength", 4));

        assertEquals(12, config.getScore());
        assertEquals(1.2, config.getInitSpeed());
        assertEquals(4, config.getInitStrLength());
        verify(monsterConfigRepository).save(config);
    }
}
