package com.typinggame.dto.dictionary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 内部接口：一条词条（游戏侧只关心文本与难度）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryWordItemDTO {
    private String word;
    private Integer difficultyLevel;
}
