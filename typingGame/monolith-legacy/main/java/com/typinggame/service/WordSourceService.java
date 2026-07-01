package com.typinggame.service;

import java.util.List;

/**
 * 词源服务：封装两条词源路径，供怪物生成管线按模式选用。
 * - 无尽适应模式：CharacterStats + StringParent（自动生成字符/字符串池）。
 * - 词源界定模式：用户自定义词库 + 系统词库（来自上传与系统词库表）。
 */
public interface WordSourceService {

    /**
     * 获取候选词源列表（用于本轮怪物 targetStr 或下一轮词生成）。
     *
     * @param userId         用户 ID
     * @param gameMode       游戏模式（ENDLESS_ADAPT / WORD_SOURCE）
     * @param limit          最多返回数量
     * @param customDictIds  词源界定模式下选中的自定义词库 ID 列表（可空）
     * @param systemDictIds  词源界定模式下选中的系统词库 ID 列表（可空）
     * @return 候选字符串列表，长度可不足 limit（词库不足时）
     */
    List<String> getCandidateWords(Integer userId, String gameMode, int limit,
                                   List<Integer> customDictIds, List<Integer> systemDictIds);
}
