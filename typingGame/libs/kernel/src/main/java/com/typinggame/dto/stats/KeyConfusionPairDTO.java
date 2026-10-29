package com.typinggame.dto.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一个「本该打 A 却打成 B」的混淆对（个人页回看与复盘展示用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyConfusionPairDTO {
    @JsonProperty("expectedChar")
    private String expectedChar; // 本该输入的字符
    @JsonProperty("typedChar")
    private String typedChar; // 实际打成什么
    @JsonProperty("count")
    private Integer count; // 累计混淆次数
}
