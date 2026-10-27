package com.typinggame.game.service.word;

import com.typinggame.enums.GameMode;
import com.typinggame.game.repository.MonsterDataRepository;
import com.typinggame.game.repository.StringParentRepository;
import com.typinggame.game.rule.DifficultyEngine;
import com.typinggame.game.rule.DifficultyRule;
import com.typinggame.game.rule.GeneticEngine;
import com.typinggame.game.rule.GeneticRule;
import com.typinggame.game.rule.RuleConfigProvider;
import com.typinggame.game.service.KeyConfusionService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class EndlessAdaptiveWordSourceStrategy implements WordSourceStrategy {

    private static final List<String> LOWER = Arrays.asList("a", "s", "d", "f", "j", "k", "l", "e", "r", "t", "i", "o", "n", "m");
    private static final List<String> UPPER = Arrays.asList("A", "S", "D", "F", "J", "K", "L", "E", "R", "T", "I", "O", "N", "M");
    private static final List<String> NUMBER = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0");
    private static final List<String> PUNCT = Arrays.asList(".", ",", ";", ":", "?", "!");
    private static final List<String> OPERATOR = Arrays.asList("+", "-", "*", "/", "=", "<", ">");
    private static final List<String> CHINESE = Arrays.asList("你", "我", "他", "中", "文", "打", "字", "练", "习");
    private static final List<String> SPECIAL = Arrays.asList("@", "#", "$", "%", "&", "_");
    /** 定向变异：最多取多少个易错字符 */
    private static final int CONFUSION_EXPECTED_LIMIT = 20;
    /** 定向变异：每个易错字符最多带几个常打错的替代字符 */
    private static final int CONFUSION_TYPED_LIMIT = 3;

    private final StringParentRepository stringParentRepository;
    private final MonsterDataRepository monsterDataRepository;
    private final RuleConfigProvider ruleConfigProvider;
    private final KeyConfusionService keyConfusionService;

    public EndlessAdaptiveWordSourceStrategy(StringParentRepository stringParentRepository,
                                             MonsterDataRepository monsterDataRepository,
                                             RuleConfigProvider ruleConfigProvider,
                                             KeyConfusionService keyConfusionService) {
        this.stringParentRepository = stringParentRepository;
        this.monsterDataRepository = monsterDataRepository;
        this.ruleConfigProvider = ruleConfigProvider;
        this.keyConfusionService = keyConfusionService;
    }

    @Override
    public boolean supports(GameMode gameMode) {
        return gameMode == GameMode.ENDLESS_ADAPT || gameMode == GameMode.LIMIT_TIME;
    }

    @Override
    public List<String> generateWords(WordSourceContext context) {
        Set<String> pool = new LinkedHashSet<>();
        List<String> parentStrings = loadParentStrings(context.getUserId());
        pool.addAll(parentStrings);
        pool.addAll(context.getFocusChars());
        List<String> charTypes = context.getCharTypes().isEmpty() ? List.of("lower", "upper", "number", "punct") : context.getCharTypes();
        for (String type : charTypes) {
            pool.addAll(poolByType(type));
        }
        if (pool.isEmpty()) {
            pool.addAll(LOWER);
        }
        GeneticRule genetic = ruleConfigProvider.activeGenetic();
        List<String> limitedParents = GeneticEngine.limitParents(parentStrings, genetic.getCrossRoundParentLimit());
        Map<String, List<String>> confusion = keyConfusionService.getConfusionMap(context.getUserId(),
                CONFUSION_EXPECTED_LIMIT, CONFUSION_TYPED_LIMIT);
        if (context.getGameMode() == GameMode.ENDLESS_ADAPT && limitedParents.size() >= 2) {
            List<String> evolved = evolveCrossRound(limitedParents, context, genetic, confusion,
                    context.getCount(), context.getLength());
            if (evolved.size() >= context.getCount()) {
                return evolved.subList(0, context.getCount());
            }
            return generateEvolvedWords(limitedParents, context.getFocusChars(), confusion,
                    context.getCount(), context.getLength(), new ArrayList<>(pool), genetic);
        }
        return buildFromPool(new ArrayList<>(pool), context.getFocusChars(), context.getCount(), context.getLength());
    }

    /**
     * 跨局进化：父本是历史字符串，只带顺序、没有局内适应度，因此精英比例为 0、锦标赛规模 1。
     * 随机源用「用户 + 父本 + 目标参数」做种子，同一份历史与配置可以复现同一批怪物。
     */
    private List<String> evolveCrossRound(List<String> parents, WordSourceContext context,
                                          GeneticRule genetic, Map<String, List<String>> confusion,
                                          int count, int length) {
        int safeCount = Math.max(1, count);
        int safeLength = Math.max(1, length);
        Random random = new Random(Objects.hash(context.getUserId(), parents, safeCount, safeLength));
        List<GeneticEngine.Parent> parentList = parents.stream()
                .map(text -> new GeneticEngine.Parent(text, 0))
                .toList();
        return GeneticEngine.evolve(new GeneticEngine.Request(
                parentList,
                context.getFocusChars(),
                confusion,
                safeCount,
                safeLength,
                genetic.getCrossRoundMutationRate(),
                0,
                1,
                genetic.getCrossover(),
                random));
    }

    private List<String> generateEvolvedWords(List<String> parents, List<String> weakChars,
                                               Map<String, List<String>> confusion,
                                               int count, int length, List<String> fallbackPool, GeneticRule genetic) {
        int safeCount = Math.max(3, Math.min(25, count));
        int safeLength = Math.max(1, Math.min(15, length));
        Set<String> result = new LinkedHashSet<>(GeneticEngine.evolve(new GeneticEngine.Request(
                parents.stream().map(text -> new GeneticEngine.Parent(text, 0)).toList(),
                weakChars,
                confusion,
                safeCount,
                safeLength,
                genetic.getCrossRoundMutationRate(),
                0,
                1,
                genetic.getCrossover(),
                new Random(Objects.hash("cross-round-fallback", parents, safeCount, safeLength)))));
        int index = 0;
        while (result.size() < safeCount) {
            result.add(buildFromPool(fallbackPool, weakChars, index + 1, safeLength).get(index));
            index++;
        }
        return new ArrayList<>(result).subList(0, safeCount);
    }

    private List<String> loadParentStrings(Integer userId) {
        if (userId == null) return Collections.emptyList();
        return stringParentRepository.findByUserId(userId)
                .map(parent -> parseIds(parent.getStrParents()))
                .filter(ids -> !ids.isEmpty())
                .map(ids -> monsterDataRepository.findByMonsterIdIn(ids).stream()
                        .map(monster -> monster.getTargetStr())
                        .filter(value -> value != null && !value.isBlank())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private List<Integer> parseIds(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        List<Integer> ids = new ArrayList<>();
        for (String item : value.split(",")) {
            try {
                ids.add(Integer.parseInt(item.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private List<String> poolByType(String type) {
        if (type == null) return List.of();
        return switch (type.trim().toLowerCase()) {
            case "upper" -> UPPER;
            case "number" -> NUMBER;
            case "punct" -> PUNCT;
            case "operator" -> OPERATOR;
            case "chinese" -> CHINESE;
            case "special" -> SPECIAL;
            default -> LOWER;
        };
    }

    private List<String> buildFromPool(List<String> pool, List<String> focusChars, int count, int length) {
        List<String> words = new ArrayList<>();
        int safeCount = Math.max(3, Math.min(25, count));
        int safeLength = Math.max(1, Math.min(15, length));
        if (pool.isEmpty()) {
            return words;
        }
        DifficultyRule rule = ruleConfigProvider.activeDifficulty();
        double multiplier = DifficultyEngine.weakExposureMultiplier(rule, focusChars == null ? 0 : focusChars.size());
        Set<String> boosted = new HashSet<>(focusChars == null ? List.of() : focusChars);
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < safeCount; i++) {
            StringBuilder word = new StringBuilder();
            for (int j = 0; j < safeLength; j++) {
                word.append(DifficultyEngine.pickWeighted(pool, boosted, multiplier, random));
            }
            words.add(word.toString());
        }
        return words;
    }
}
