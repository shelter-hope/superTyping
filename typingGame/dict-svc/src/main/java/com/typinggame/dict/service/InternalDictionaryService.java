package com.typinggame.dict.service;

import com.typinggame.dto.dictionary.DictionaryPracticeUpdateDTO;
import com.typinggame.dto.dictionary.DictionaryWordItemDTO;

import java.util.Collection;
import java.util.List;

/**
 * 词库服务的内部接口（供 game-core-svc 在对局取词/结算时调用）。
 * 词表与练习统计都只由 dict-svc 读写，避免其它服务直连词库表。
 */
public interface InternalDictionaryService {

    /** 过滤出属于该用户的自定义词典 ID（越权词典直接丢弃）。 */
    List<Integer> allowedCustomDictIds(Collection<Integer> dictIds, Integer userId);

    /** 过滤出仍然启用的系统词典 ID。 */
    List<Integer> activeSystemDictIds(Collection<Integer> dictIds);

    /**
     * 按词库取词。
     *
     * @param source CUSTOM / SYSTEM
     * @param words  非空时只返回这些词
     * @param limit  最多返回多少条（默认 500）
     */
    List<DictionaryWordItemDTO> words(String source, Collection<Integer> dictIds, Collection<String> words, Integer limit);

    /** 结算时回写自定义词库词的练习/错误次数，返回更新条数。 */
    int updatePractice(DictionaryPracticeUpdateDTO update);
}
