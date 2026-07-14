package com.typinggame.game.service.word;

import com.typinggame.enums.GameMode;

import java.util.List;

public interface WordSourceStrategy {
    boolean supports(GameMode gameMode);

    List<String> generateWords(WordSourceContext context);
}
