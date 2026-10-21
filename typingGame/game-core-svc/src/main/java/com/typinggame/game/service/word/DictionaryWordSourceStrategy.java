package com.typinggame.game.service.word;

import com.typinggame.entity.dictionaries.CustomDictionary;
import com.typinggame.entity.dictionaries.DictionaryWord;
import com.typinggame.entity.dictionaries.SystemDictionary;
import com.typinggame.entity.dictionaries.SystemDictionaryWord;
import com.typinggame.enums.GameMode;
import com.typinggame.game.repository.GameCustomDictionaryRepository;
import com.typinggame.game.repository.GameDictionaryWordRepository;
import com.typinggame.game.repository.GameSystemDictionaryRepository;
import com.typinggame.game.repository.GameSystemDictionaryWordRepository;
import com.typinggame.game.word.WordDraw;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
public class DictionaryWordSourceStrategy implements WordSourceStrategy {

    private final GameCustomDictionaryRepository customDictionaryRepository;
    private final GameDictionaryWordRepository dictionaryWordRepository;
    private final GameSystemDictionaryWordRepository systemDictionaryWordRepository;
    private final GameSystemDictionaryRepository systemDictionaryRepository;
    private final EndlessAdaptiveWordSourceStrategy fallbackStrategy;

    public DictionaryWordSourceStrategy(GameCustomDictionaryRepository customDictionaryRepository,
                                        GameDictionaryWordRepository dictionaryWordRepository,
                                        GameSystemDictionaryWordRepository systemDictionaryWordRepository,
                                        GameSystemDictionaryRepository systemDictionaryRepository,
                                        EndlessAdaptiveWordSourceStrategy fallbackStrategy) {
        this.customDictionaryRepository = customDictionaryRepository;
        this.dictionaryWordRepository = dictionaryWordRepository;
        this.systemDictionaryWordRepository = systemDictionaryWordRepository;
        this.systemDictionaryRepository = systemDictionaryRepository;
        this.fallbackStrategy = fallbackStrategy;
    }

    @Override
    public boolean supports(GameMode gameMode) {
        return gameMode == GameMode.WORD_SOURCE;
    }

    @Override
    public List<String> generateWords(WordSourceContext context) {
        List<WordDraw.Entry> pool = new ArrayList<>();
        List<Integer> allowedCustomIds = loadAllowedCustomDictIds(context);
        if (!allowedCustomIds.isEmpty()) {
            dictionaryWordRepository.findByDictIdIn(allowedCustomIds, page()).stream()
                    .filter(word -> validWord(word.getWord()))
                    .forEach(word -> pool.add(new WordDraw.Entry(word.getWord(), weight(word.getDifficultyLevel()))));
        }
        List<Integer> activeSystemIds = activeSystemIds(context.getSystemDictIds());
        if (!activeSystemIds.isEmpty()) {
            systemDictionaryWordRepository.findByDictIdIn(activeSystemIds, page()).stream()
                    .filter(word -> validWord(word.getWord()))
                    .forEach(word -> pool.add(new WordDraw.Entry(word.getWord(), weight(word.getDifficultyLevel()))));
        }
        if (pool.isEmpty()) {
            return fallbackStrategy.generateWords(context);
        }
        return WordDraw.draw(pool, context.getCount(), ThreadLocalRandom.current());
    }

    private List<Integer> activeSystemIds(List<Integer> requested) {
        if (requested == null || requested.isEmpty()) return List.of();
        List<Integer> active = systemDictionaryRepository.findByDictIdInAndIsActiveTrue(requested).stream()
                .map(SystemDictionary::getDictId)
                .collect(Collectors.toList());
        return WordDraw.activeOnly(requested, active);
    }

    private int weight(Integer difficultyLevel) {
        return difficultyLevel == null ? 1 : difficultyLevel;
    }

    private List<Integer> loadAllowedCustomDictIds(WordSourceContext context) {
        if (context.getCustomDictIds().isEmpty()) return List.of();
        return customDictionaryRepository.findByDictIdInAndUserId(context.getCustomDictIds(), context.getUserId()).stream()
                .map(CustomDictionary::getDictId)
                .collect(Collectors.toList());
    }

    private PageRequest page() {
        return PageRequest.of(0, 500);
    }

    private boolean validWord(String word) {
        return word != null && !word.isBlank() && word.length() <= 50;
    }

}
