package com.typinggame.game.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.dto.game.GameRoundCompleteDTO;
import com.typinggame.dto.game.GameRoundCompleteResultDTO;
import com.typinggame.dto.game.MonsterDataDTO;
import com.typinggame.game.entity.GameRound;
import com.typinggame.game.entity.GameSession;
import com.typinggame.game.entity.MonsterAttempt;
import com.typinggame.game.entity.UserModelAdaptLog;
import com.typinggame.game.repository.GameRoundRepository;
import com.typinggame.game.repository.GameSessionRepository;
import com.typinggame.game.repository.MonsterAttemptRepository;
import com.typinggame.game.repository.MonsterParentRelationRepository;
import com.typinggame.game.repository.UserModelAdaptLogRepository;
import com.typinggame.game.service.rule.RuleConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameRoundServiceImplTest {
    private GameSessionRepository sessionRepository;
    private GameRoundRepository roundRepository;
    private MonsterAttemptRepository attemptRepository;
    private UserModelAdaptLogRepository adaptLogRepository;
    private GameRoundServiceImpl service;
    private GameRound round;

    @BeforeEach
    void setUp() throws Exception {
        sessionRepository = mock(GameSessionRepository.class);
        roundRepository = mock(GameRoundRepository.class);
        attemptRepository = mock(MonsterAttemptRepository.class);
        adaptLogRepository = mock(UserModelAdaptLogRepository.class);
        service = new GameRoundServiceImpl(
                sessionRepository,
                roundRepository,
                attemptRepository,
                mock(MonsterParentRelationRepository.class),
                adaptLogRepository,
                new ObjectMapper(),
                RuleConfigProvider.DEFAULTS);

        GameSession session = new GameSession();
        session.setSessionId(10L);
        session.setUserId(7);
        session.setGameMode("ENDLESS");
        session.setCurrentRound(0);
        when(sessionRepository.findBySessionIdAndUserId(10L, 7)).thenReturn(Optional.of(session));
        when(adaptLogRepository.save(any(UserModelAdaptLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MonsterDataDTO planMonster = new MonsterDataDTO();
        planMonster.setMonsterKey("round-1-monster-0");
        planMonster.setMonsterType("NORMAL");
        planMonster.setTargetStr("abc");
        planMonster.setScore(20);
        planMonster.setGeneration(0);

        round = new GameRound();
        round.setRoundId(100L);
        round.setSessionId(10L);
        round.setRoundNum(1);
        round.setPlanVersion(3L);
        round.setPlanPayload(new ObjectMapper().writeValueAsString(List.of(planMonster)));
        round.setStatus("CREATED");
        when(roundRepository.findBySessionIdAndRoundNum(10L, 1)).thenReturn(Optional.of(round));
        when(attemptRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void completesRoundAndCalculatesVerifiedMetrics() {
        GameRoundCompleteResultDTO result = service.completeRound(7, request(monster("round-1-monster-0", "KILLED", 3, 3, 0)));

        assertEquals(new BigDecimal("1.0000"), result.getVerifiedAccuracy());
        assertEquals(new BigDecimal("1.0000"), result.getKillRate());
        assertEquals(20, result.getVerifiedScore());
        assertEquals("COMPLETED", round.getStatus());
        assertNotNull(round.getDifficultyAfter());

        ArgumentCaptor<UserModelAdaptLog> captor = ArgumentCaptor.forClass(UserModelAdaptLog.class);
        verify(adaptLogRepository).save(captor.capture());
        UserModelAdaptLog log = captor.getValue();
        assertNull(log.getGameRecord());
        assertEquals("ENDLESS", log.getGameMode());
        assertEquals(10L, log.getSessionId());
        assertEquals("HIGH_ACCURACY_ADVANCE", log.getTriggerRule());
        assertNotNull(log.getAdjustParams());
    }

    @Test
    void repeatedCompletionReturnsExistingResult() {
        MonsterAttempt existing = new MonsterAttempt();
        existing.setResult("KILLED");
        existing.setVerifiedScore(20);
        existing.setInputCount(3);
        existing.setCorrectCount(3);
        when(attemptRepository.findByRoundId(100L)).thenReturn(List.of(existing));

        service.completeRound(7, request(monster("round-1-monster-0", "KILLED", 3, 3, 0)));
        GameRoundCompleteResultDTO result = service.completeRound(7, request(monster("round-1-monster-0", "KILLED", 3, 3, 0)));

        assertEquals(20, result.getVerifiedScore());
        assertEquals("COMPLETED", result.getStatus());
    }

    @Test
    void rejectsMonsterOutsideRoundPlan() {
        assertThrows(IllegalArgumentException.class,
                () -> service.completeRound(7, request(monster("other", "KILLED", 3, 3, 0))));
    }

    private GameRoundCompleteDTO request(MonsterDataDTO monster) {
        GameRoundCompleteDTO request = new GameRoundCompleteDTO();
        request.setSessionId(10L);
        request.setRoundNum(1);
        request.setPlanVersion(3L);
        request.setMonsters(List.of(monster));
        return request;
    }

    private MonsterDataDTO monster(String key, String result, int input, int correct, int errors) {
        MonsterDataDTO monster = new MonsterDataDTO();
        monster.setMonsterKey(key);
        monster.setMonsterType("NORMAL");
        monster.setTargetStr("abc");
        monster.setResult(result);
        monster.setInputCount(input);
        monster.setCorrectCount(correct);
        monster.setErrorCount(errors);
        return monster;
    }
}
