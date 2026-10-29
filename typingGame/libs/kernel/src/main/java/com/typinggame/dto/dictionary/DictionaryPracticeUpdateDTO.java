package com.typinggame.dto.dictionary;

import lombok.Data;

import java.util.List;

/** 内部接口：把一局里自定义词库词的练习/错误次数交回词库服务累计。 */
@Data
public class DictionaryPracticeUpdateDTO {
    private List<Integer> dictIds;
    private List<PracticeItem> items;

    @Data
    public static class PracticeItem {
        private String word;
        private Integer practiceCount;
        private Integer errorCount;
    }
}
