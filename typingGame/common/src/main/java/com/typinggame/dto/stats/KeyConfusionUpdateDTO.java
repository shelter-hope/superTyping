package com.typinggame.dto.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 单个按键混淆增量（前端结算时聚合上报，一次一条）
 */
@Data
public class KeyConfusionUpdateDTO {
    @JsonProperty("expectedChar")
    private String expectedChar; // 本该输入的字符
    @JsonProperty("typedChar")
    private String typedChar; // 实际打成什么
    @JsonProperty("count")
    private Integer count; // 本轮次数
}
