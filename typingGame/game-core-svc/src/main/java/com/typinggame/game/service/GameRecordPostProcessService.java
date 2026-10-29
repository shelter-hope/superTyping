package com.typinggame.game.service;

import com.typinggame.dto.game.GameRecordUploadDTO;
import com.typinggame.game.entity.GameRecord;
import com.typinggame.game.entity.MonsterData;

import java.math.BigDecimal;
import java.util.List;

public interface GameRecordPostProcessService {
    BigDecimal process(Integer userId, GameRecord record, GameRecordUploadDTO uploadDTO, List<MonsterData> monsters);
}
