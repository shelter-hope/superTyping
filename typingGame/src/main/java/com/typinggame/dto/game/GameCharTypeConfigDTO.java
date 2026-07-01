package com.typinggame.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前用户在某游戏模式下的字符类型配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameCharTypeConfigDTO {
    /** 前端模式字符串：endless/dictionary/limit */
    private String gameMode;
    /** 逗号分隔的类型字符串：lower,upper,number,punct,operator,chinese,special */
    private String charType;
    /** 规范化后的类型列表 */
    private List<String> charTypes;
}

