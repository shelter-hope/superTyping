package com.typinggame.service;

import com.typinggame.entity.game.SystemMonsterConfig;
import java.util.List;

public interface MonsterConfigService {
    List<SystemMonsterConfig> getAllConfigs();
    SystemMonsterConfig updateConfig(String type, SystemMonsterConfig updates);
}