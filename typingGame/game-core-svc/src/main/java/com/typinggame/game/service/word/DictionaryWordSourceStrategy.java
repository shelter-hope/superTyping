package com.typinggame.game.service.word;

import com.typinggame.entity.dictionaries.CustomDictionary;
import com.typinggame.entity.dictionaries.DictionaryWord;
import com.typinggame.entity.dictionaries.SystemDictionaryWord;
import com.typinggame.enums.GameMode;
import com.typinggame.game.repository.GameCustomDictionaryRepository;
import com.typinggame.game.repository.GameDictionaryWordRepository;
import com.typinggame.game.repository.GameSystemDictionaryWordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DictionaryWordSourceStrategy implements WordSourceStrategy {

    private final GameCustomDictionaryRepository customDictionaryRepository;
    private final GameDictionaryWordRepository dictionaryWordRepository;
    private final GameSystemDictionaryWordRepository systemDictionaryWordRepository;
    private final EndlessAdaptiveWordSourceStrategy fallbackStrategy;

    public DictionaryWordSourceStrategy(GameCustomDictionaryRepository customDictionaryRepository,
                                        GameDictionaryWordRepository dictionaryWordRepository,
                                        GameSystemDictionaryWordRepository systemDictionaryWordRepository,
                                        EndlessAdaptiveWordSourceStrategy fallbackStrategy) {
        this.customDictionaryRepository = customDictionaryRepository;
        this.dictionaryWordRepository = dictionaryWordRepository;
        this.systemDictionaryWordRepository = systemDictionaryWordRepository;
        this.fallbackStrategy = fallbackStrategy;
    }

    @Override
    public boolean supports(GameMode gameMode) {
        return gameMode == GameMode.WORD_SOURCE;
    }

    @Override
    public List<String> generateWords(WordSourceContext context) {
        Set<String> words = new LinkedHashSet<>();
        List<Integer> allowedCustomIds = loadAllowedCustomDictIds(context);
        if (!allowedCustomIds.isEmpty()) {
            words.addAll(dictionaryWordRepository.findByDictIdIn(allowedCustomIds, page(context.getCount() * 3)).stream()
                    .map(DictionaryWord::getWord)
                    .filter(this::validWord)
                    .collect(Collectors.toList()));
        }
        if (!context.getSystemDictIds().isEmpty()) {
            words.addAll(systemDictionaryWordRepository.findByDictIdIn(context.getSystemDictIds(), page(context.getCount() * 3)).stream()
                    .map(SystemDictionaryWord::getWord)
                    .filter(this::validWord)
                    .collect(Collectors.toList()));
        }
        if (words.isEmpty()) {
            return fallbackStrategy.generateWords(context);
        }
        return normalizeCount(new ArrayList<>(words), context.getCount());
    }

    private List<Integer> loadAllowedCustomDictIds(WordSourceContext context) {
        if (context.getCustomDictIds().isEmpty()) return List.of();
        return customDictionaryRepository.findByDictIdInAndUserId(context.getCustomDictIds(), context.getUserId()).stream()
                .map(CustomDictionary::getDictId)
                .collect(Collectors.toList());
    }

    private PageRequest page(int size) {
        return PageRequest.of(0, Math.max(10, Math.min(200, size)), Sort.by(Sort.Direction.ASC, "wordId"));
    }

    private boolean validWord(String word) {
        return word != null && !word.isBlank() && word.length() <= 50;
    }

    private List<String> normalizeCount(List<String> source, int count) {
        int safeCount = Math.max(3, Math.min(25, count));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < safeCount; i++) {
            result.add(source.get(i % source.size()));
        }
        return result;
    }
}
