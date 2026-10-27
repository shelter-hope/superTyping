package com.typinggame.game.service;

import com.typinggame.dto.stats.KeyConfusionUpdateDTO;

import java.util.List;
import java.util.Map;

/**
 * 按键混淆档案：把「该打 A 却打成 B」的统计落库，并回喂给遗传算法的定向变异。
 */
public interface KeyConfusionService {

    /** 结算时批量累加本局混淆数据（幂等累加，单条次数有上限） */
    void batchUpdate(Integer userId, List<KeyConfusionUpdateDTO> updates);

    /**
     * 取该用户最强的混淆映射：expectedChar → 按次数降序的 typedChar 列表。
     *
     * @param maxExpectedChars 最多返回多少个「易错字符」
     * @param maxTypedPerExpected 每个易错字符最多带几个「常打错成」
     */
    Map<String, List<String>> getConfusionMap(Integer userId, int maxExpectedChars, int maxTypedPerExpected);

    /** 条数上限校验用：返回该用户档案总条数 */
    long countByUserId(Integer userId);
}
