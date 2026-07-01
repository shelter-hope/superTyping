package com.typinggame.service.impl;

import com.typinggame.entity.game.SystemMonsterConfig;
import com.typinggame.repository.SystemMonsterConfigRepository;
import com.typinggame.service.MonsterConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MonsterConfigServiceImpl implements MonsterConfigService {

    @Autowired
    private SystemMonsterConfigRepository configRepository;

    @Override
    public List<SystemMonsterConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    @Override
    public SystemMonsterConfig updateConfig(String typeStr, SystemMonsterConfig updates) {
        try {
            String type = typeStr.toUpperCase();
            return configRepository.findByType(type).map(config -> {
                if (updates.getScore() != null) config.setScore(updates.getScore());
                if (updates.getInitSpeed() != null) config.setInitSpeed(updates.getInitSpeed());
                if (updates.getInitStrLength() != null) config.setInitStrLength(updates.getInitStrLength());
                if (updates.getDescription() != null) config.setDescription(updates.getDescription());
                return configRepository.save(config);
            }).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}