package com.typinggame.game.service.word;

import com.typinggame.enums.GameMode;
import com.typinggame.game.rule.DifficultyEngine;
import com.typinggame.game.rule.RuleConfigProvider;
import com.typinggame.game.word.WordDraw;
import com.typinggame.game.word.WordSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DictionaryWordSourceStrategy implements WordSourceStrategy {

    private final WordSource wordSource;
    private final EndlessAdaptiveWordSourceStrategy fallbackStrategy;
    private final RuleConfigProvider ruleConfigProvider;

    public DictionaryWordSourceStrategy(WordSource wordSource,
                                        EndlessAdaptiveWordSourceStrategy fallbackStrategy,
                                        RuleConfigProvider ruleConfigProvider) {
        this.wordSource = wordSource;
        this.fallbackStrategy = fallbackStrategy;
        this.ruleConfigProvider = ruleConfigProvider;
    }

    @Override
    public boolean supports(GameMode gameMode) {
        return gameMode == GameMode.WORD_SOURCE;
    }

    @Override
    public List<String> generateWords(WordSourceContext context) {
        List<WordDraw.Entry> pool = new ArrayList<>();
        double multiplier = DifficultyEngine.weakExposureMultiplier(
                ruleConfigProvider.activeDifficulty(), context.getFocusChars().size());
        List<Integer> allowedCustomIds = wordSource.allowedCustomDictIds(context.getCustomDictIds(), context.getUserId());
        if (!allowedCustomIds.isEmpty()) {
            wordSource.customWords(allowedCustomIds).stream()
                    .filter(word -> validWord(word.word()))
                    .forEach(word -> pool.add(new WordDraw.Entry(word.word(),
                            DifficultyEngine.exposureWeight(
                                    word.word(), context.getFocusChars(), weight(word.difficultyLevel()), multiplier))));
        }
        List<Integer> activeSystemIds = WordDraw.activeOnly(
                context.getSystemDictIds(), wordSource.activeSystemDictIds(context.getSystemDictIds()));
        if (!activeSystemIds.isEmpty()) {
            wordSource.systemWords(activeSystemIds).stream()
                    .filter(word -> validWord(word.word()))
                    .forEach(word -> pool.add(new WordDraw.Entry(word.word(),
                            DifficultyEngine.exposureWeight(
                                    word.word(), context.getFocusChars(), weight(word.difficultyLevel()), multiplier))));
        }
        if (pool.isEmpty()) {
            return fallbackStrategy.generateWords(context);
        }
        return WordDraw.draw(pool, context.getCount(), ThreadLocalRandom.current());
    }

    private int weight(Integer difficultyLevel) {
        return difficultyLevel == null ? 1 : difficultyLevel;
    }

    private boolean validWord(String word) {
        return word != null && !word.isBlank() && word.length() <= 50;
    }
}
