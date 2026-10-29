package com.typinggame.game.service.word;

import java.util.Collection;
import java.util.List;

/**
 * 词库读端口。热路径当前仍走本地 JPA；日后可换成 HTTP/缓存实现而不改对局策略。
 */
public interface WordSource {
    List<Integer> allowedCustomDictIds(Collection<Integer> dictIds, Integer userId);

    List<Integer> activeSystemDictIds(Collection<Integer> dictIds);

    List<WordEntry> customWords(Collection<Integer> dictIds);

    List<WordEntry> systemWords(Collection<Integer> dictIds);

    record WordEntry(String word, Integer difficultyLevel) {
    }
}
