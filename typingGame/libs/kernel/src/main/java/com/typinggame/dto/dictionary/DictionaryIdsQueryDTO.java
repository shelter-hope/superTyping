package com.typinggame.dto.dictionary;

import lombok.Data;

import java.util.List;

/** 内部接口：查询一批词典里可用的词库 ID（自定义词典需要 userId 校验归属）。 */
@Data
public class DictionaryIdsQueryDTO {
    /** CUSTOM / SYSTEM */
    private String source;
    private List<Integer> dictIds;
    private Integer userId;
}
