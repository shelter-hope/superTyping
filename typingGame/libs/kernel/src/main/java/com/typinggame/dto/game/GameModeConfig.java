package com.typinggame.dto.game;

import com.typinggame.enums.CharType;
import com.typinggame.enums.GameMode;
import lombok.Data;
import java.util.List;

/**
 * 游戏模式配置（玩家选择的游戏参数）
 */
@Data
public class GameModeConfig {
    private GameMode gameMode;          // 游戏模式（无尽适应/词源界定）
    private List<CharType> selectedCharTypes; // 选中的字符类型
    private String customWordSource;    // 自定义词源数据（词源界定模式专用）

}