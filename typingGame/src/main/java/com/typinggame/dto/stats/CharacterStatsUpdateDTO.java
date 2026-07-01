package com.typinggame.dto.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 单个字符统计更新请求（前端按击键顺序上传，每条一条）
 */
@Data
public class CharacterStatsUpdateDTO {
    @JsonProperty("charValue")
    private String charValue;
    @JsonProperty("isCorrect")
    private Boolean isCorrect;
    @JsonProperty("charType")
    private String charType;
}
