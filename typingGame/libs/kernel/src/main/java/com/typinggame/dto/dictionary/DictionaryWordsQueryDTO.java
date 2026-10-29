package com.typinggame.dto.dictionary;

import lombok.Data;

import java.util.List;

/** 内部接口：按词库 ID 拉取词条（words 非空时只返回这些词，用于练习统计定位）。 */
@Data
public class DictionaryWordsQueryDTO {
    /** CUSTOM / SYSTEM */
    private String source;
    private List<Integer> dictIds;
    private List<String> words;
    private Integer limit;
}
