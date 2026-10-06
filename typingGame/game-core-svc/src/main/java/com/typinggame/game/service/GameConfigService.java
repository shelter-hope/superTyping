package com.typinggame.game.service;

import com.typinggame.dto.game.GameCharTypeConfigDTO;
import com.typinggame.dto.game.GameCharTypeConfigUpdateDTO;
import com.typinggame.dto.game.GameRecommendDTO;
import com.typinggame.dto.game.NextRoundResponseDTO;
import com.typinggame.dto.game.GameSessionCreateDTO;
import com.typinggame.dto.game.GameSessionDTO;

import java.util.List;

public interface GameConfigService {
    GameCharTypeConfigDTO getCharTypes(Integer userId, String gameMode);

    boolean updateCharTypes(Integer userId, GameCharTypeConfigUpdateDTO request);

    GameRecommendDTO getRecommend(Integer userId, String gameMode);

    NextRoundResponseDTO getNextRound(Integer userId, int roundNum, String gameMode, List<Integer> customDictIds, List<Integer> systemDictIds);

    NextRoundResponseDTO getNextRound(Integer userId, Long sessionId, int roundNum, String gameMode,
                                      List<Integer> customDictIds, List<Integer> systemDictIds);

    GameSessionDTO createSession(Integer userId, GameSessionCreateDTO request);
}
