package com.typinggame.game.service.word;

import com.typinggame.enums.GameMode;

import java.util.Collections;
import java.util.List;

public class WordSourceContext {
    private final Integer userId;
    private final GameMode gameMode;
    private final int count;
    private final int length;
    private final List<String> focusChars;
    private final List<String> charTypes;
    private final List<Integer> customDictIds;
    private final List<Integer> systemDictIds;

    public WordSourceContext(Integer userId, GameMode gameMode, int count, int length, List<String> focusChars,
                             List<String> charTypes, List<Integer> customDictIds, List<Integer> systemDictIds) {
        this.userId = userId;
        this.gameMode = gameMode;
        this.count = count;
        this.length = length;
        this.focusChars = focusChars == null ? Collections.emptyList() : focusChars;
        this.charTypes = charTypes == null ? Collections.emptyList() : charTypes;
        this.customDictIds = customDictIds == null ? Collections.emptyList() : customDictIds;
        this.systemDictIds = systemDictIds == null ? Collections.emptyList() : systemDictIds;
    }

    public Integer getUserId() { return userId; }
    public GameMode getGameMode() { return gameMode; }
    public int getCount() { return count; }
    public int getLength() { return length; }
    public List<String> getFocusChars() { return focusChars; }
    public List<String> getCharTypes() { return charTypes; }
    public List<Integer> getCustomDictIds() { return customDictIds; }
    public List<Integer> getSystemDictIds() { return systemDictIds; }
}
