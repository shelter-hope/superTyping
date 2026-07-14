package com.typinggame.game.service.word;

import com.typinggame.enums.GameMode;
import com.typinggame.game.repository.MonsterDataRepository;
import com.typinggame.game.repository.StringParentRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    private final StringParentRepository stringParentRepository;
    private final MonsterDataRepository monsterDataRepository;

    public EndlessAdaptiveWordSourceStrategy(StringParentRepository stringParentRepository, MonsterDataRepository monsterDataRepository) {
        this.stringParentRepository = stringParentRepository;
        this.monsterDataRepository = monsterDataRepository;
    }

    @Override
    public boolean supports(GameMode gameMode) {
        return gameMode == GameMode.ENDLESS_ADAPT || gameMode == GameMode.LIMIT_TIME;
    }

    @Override
    public List<String> generateWords(WordSourceContext context) {
        Set<String> pool = new LinkedHashSet<>();
        pool.addAll(loadParentStrings(context.getUserId()));
        pool.addAll(context.getFocusChars());
        List<String> charTypes = context.getCharTypes().isEmpty() ? List.of("lower", "upper", "number", "punct") : context.getCharTypes();
        for (String type : charTypes) {
            pool.addAll(poolByType(type));
        }
        if (pool.isEmpty()) {
            pool.addAll(LOWER);
        }
        return buildFromPool(new ArrayList<>(pool), context.getCount(), context.getLength());
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

    private List<String> buildFromPool(List<String> pool, int count, int length) {
        List<String> words = new ArrayList<>();
        int safeCount = Math.max(3, Math.min(25, count));
        int safeLength = Math.max(1, Math.min(15, length));
        for (int i = 0; i < safeCount; i++) {
            StringBuilder word = new StringBuilder();
            for (int j = 0; j < safeLength; j++) {
                word.append(pool.get((i + j) % pool.size()));
            }
            words.add(word.toString());
        }
        return words;
    }
}
