package com.typinggame.game.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.dto.game.GameRoundCompleteDTO;
import com.typinggame.dto.game.GameRoundCompleteResultDTO;
import com.typinggame.dto.game.MonsterDataDTO;
import com.typinggame.entity.game.GameRound;
import com.typinggame.entity.game.GameSession;
import com.typinggame.entity.game.MonsterAttempt;
import com.typinggame.entity.game.MonsterParentRelation;
import com.typinggame.game.repository.GameRoundRepository;
import com.typinggame.game.repository.GameSessionRepository;
import com.typinggame.game.repository.MonsterAttemptRepository;
import com.typinggame.game.repository.MonsterParentRelationRepository;
import com.typinggame.game.service.GameRoundService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GameRoundServiceImpl implements GameRoundService {
    private final GameSessionRepository gameSessionRepository;
    private final GameRoundRepository gameRoundRepository;
    private final MonsterAttemptRepository monsterAttemptRepository;
    private final MonsterParentRelationRepository parentRelationRepository;
    private final ObjectMapper objectMapper;

    public GameRoundServiceImpl(GameSessionRepository gameSessionRepository,
                                GameRoundRepository gameRoundRepository,
                                MonsterAttemptRepository monsterAttemptRepository,
                                MonsterParentRelationRepository parentRelationRepository,
                                ObjectMapper objectMapper) {
        this.gameSessionRepository = gameSessionRepository;
        this.gameRoundRepository = gameRoundRepository;
        this.monsterAttemptRepository = monsterAttemptRepository;
        this.parentRelationRepository = parentRelationRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public GameRoundCompleteResultDTO completeRound(Integer userId, GameRoundCompleteDTO request) {
        GameSession session = gameSessionRepository.findBySessionIdAndUserId(request.getSessionId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("游戏会话不存在或无权访问"));
        GameRound round = gameRoundRepository.findBySessionIdAndRoundNum(request.getSessionId(), request.getRoundNum())
                .orElseThrow(() -> new IllegalArgumentException("游戏轮次不存在"));
        if (!request.getPlanVersion().equals(round.getPlanVersion())) {
            throw new IllegalArgumentException("RoundPlan版本不匹配");
        }
        if ("COMPLETED".equals(round.getStatus())) {
            return buildResult(round, monsterAttemptRepository.findByRoundId(round.getRoundId()));
        }

        List<MonsterDataDTO> planned = readPlan(round.getPlanPayload());
        List<MonsterDataDTO> reported = request.getMonsters() == null ? List.of() : request.getMonsters();
        if (reported.size() > planned.size()) {
            throw new IllegalArgumentException("上报怪物数量超过服务端计划");
        }

        Map<String, List<MonsterDataDTO>> plannedByKey = new HashMap<>();
        for (MonsterDataDTO monster : planned) {
            plannedByKey.computeIfAbsent(planKey(monster), ignored -> new ArrayList<>()).add(monster);
        }
        List<MonsterAttempt> attempts = new ArrayList<>();
        for (MonsterDataDTO item : reported) {
            List<MonsterDataDTO> candidates = plannedByKey.get(planKey(item));
            if (candidates == null || candidates.isEmpty()) {
                throw new IllegalArgumentException("上报怪物不属于服务端RoundPlan");
            }
            MonsterDataDTO plan = candidates.remove(0);
            String attemptKey = planKey(plan) + "#" + (planned.size() - candidates.size());
            int inputCount = nonNegative(item.getInputCount());
            int errorCount = nonNegative(item.getErrorCount() == null ? item.getInputErrorCount() : item.getErrorCount());
            int correctCount = Math.min(nonNegative(item.getCorrectCount()), plan.getTargetStr().length());
            if (correctCount + errorCount > inputCount) {
                throw new IllegalArgumentException("怪物输入统计不一致");
            }
            boolean killed = "KILLED".equalsIgnoreCase(item.getResult())
                    && correctCount >= plan.getTargetStr().length();
            MonsterAttempt attempt = new MonsterAttempt();
            attempt.setSessionId(session.getSessionId());
            attempt.setRoundId(round.getRoundId());
            attempt.setPlanVersion(round.getPlanVersion());
            attempt.setMonsterKey(attemptKey);
            attempt.setTargetStr(plan.getTargetStr());
            attempt.setMonsterType(plan.getMonsterType());
            attempt.setSpeed(plan.getSpeed());
            attempt.setInputCount(inputCount);
            attempt.setCorrectCount(correctCount);
            attempt.setErrorCount(errorCount);
            attempt.setResult(killed ? "KILLED" : normalizeResult(item.getResult()));
            attempt.setClientScore(item.getScore());
            attempt.setVerifiedScore(killed ? nonNegative(plan.getScore()) : 0);
            attempt.setGeneration(plan.getGeneration() == null ? 0 : plan.getGeneration());
            attempt.setCreatedAt(new Date());
            attempts.add(attempt);
        }
        attempts = monsterAttemptRepository.saveAll(attempts);
        saveParentRelations(reported, attempts);

        BigDecimal accuracy = calculateAccuracy(attempts);
        long killedCount = attempts.stream().filter(item -> "KILLED".equals(item.getResult())).count();
        BigDecimal killRate = ratio(killedCount, planned.size());
        int verifiedScore = attempts.stream().mapToInt(item -> nonNegative(item.getVerifiedScore())).sum();
        round.setActualAccuracy(accuracy);
        round.setStatus("COMPLETED");
        round.setCompletedAt(new Date());
        gameRoundRepository.save(round);
        session.setCurrentRound(Math.max(session.getCurrentRound() == null ? 0 : session.getCurrentRound(), request.getRoundNum()));
        session.setUpdatedAt(new Date());
        gameSessionRepository.save(session);
        return GameRoundCompleteResultDTO.builder()
                .roundId(round.getRoundId()).roundNum(round.getRoundNum())
                .verifiedAccuracy(accuracy).killRate(killRate).verifiedScore(verifiedScore)
                .attemptCount(attempts.size()).status(round.getStatus()).build();
    }

    private List<MonsterDataDTO> readPlan(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<List<MonsterDataDTO>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("RoundPlan快照损坏", exception);
        }
    }

    private void saveParentRelations(List<MonsterDataDTO> reported, List<MonsterAttempt> attempts) {
        for (int i = 0; i < reported.size(); i++) {
            MonsterDataDTO item = reported.get(i);
            if (item.getParentAttemptAId() == null && item.getParentAttemptBId() == null) {
                continue;
            }
            MonsterAttempt child = attempts.get(i);
            if ((item.getParentAttemptAId() != null && !monsterAttemptRepository.existsById(item.getParentAttemptAId()))
                    || (item.getParentAttemptBId() != null && !monsterAttemptRepository.existsById(item.getParentAttemptBId()))) {
                throw new IllegalArgumentException("遗传父本不存在");
            }
            MonsterParentRelation relation = new MonsterParentRelation();
            relation.setChildAttemptId(child.getAttemptId());
            relation.setParentAAttemptId(item.getParentAttemptAId());
            relation.setParentBAttemptId(item.getParentAttemptBId());
            relation.setCrossoverPoint(item.getCrossoverPoint());
            relation.setMutationRate(item.getMutationRate());
            relation.setMutationPositions(item.getMutationPositions());
            relation.setMutationChars(item.getMutationChars());
            relation.setGeneration(child.getGeneration());
            relation.setAlgorithmVersion("v1");
            relation.setCreatedAt(new Date());
            parentRelationRepository.save(relation);
        }
    }

    private GameRoundCompleteResultDTO buildResult(GameRound round, List<MonsterAttempt> attempts) {
        long killed = attempts.stream().filter(item -> "KILLED".equals(item.getResult())).count();
        return GameRoundCompleteResultDTO.builder().roundId(round.getRoundId()).roundNum(round.getRoundNum())
                .verifiedAccuracy(round.getActualAccuracy()).killRate(ratio(killed, attempts.size()))
                .verifiedScore(attempts.stream().mapToInt(item -> nonNegative(item.getVerifiedScore())).sum())
                .attemptCount(attempts.size()).status(round.getStatus()).build();
    }

    private String planKey(MonsterDataDTO monster) {
        return String.valueOf(monster.getMonsterType()) + "|" + String.valueOf(monster.getTargetStr());
    }

    private String normalizeResult(String result) {
        return result == null || result.isBlank() ? "ABANDONED" : result.toUpperCase();
    }

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private BigDecimal calculateAccuracy(List<MonsterAttempt> attempts) {
        int correct = attempts.stream().mapToInt(item -> nonNegative(item.getCorrectCount())).sum();
        int input = attempts.stream().mapToInt(item -> nonNegative(item.getInputCount())).sum();
        return ratio(correct, input);
    }

    private BigDecimal ratio(long numerator, long denominator) {
        return denominator == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }
}
