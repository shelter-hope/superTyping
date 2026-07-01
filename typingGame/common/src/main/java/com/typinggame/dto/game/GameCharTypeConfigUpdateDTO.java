package com.typinggame.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新字符类型配置请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameCharTypeConfigUpdateDTO {
    /** 前端模式字符串：endless/dictionary/limit */
    private String gameMode;
    /**
     * 逗号分隔的类型字符串（可选），优先级低于 charTypes
     * 允许：lower,upper,number,punct,operator,chinese,special
     */
    private String charType;
    /** 类型列表（可选），优先使用该字段 */
    private List<String> charTypes;
}

