package com.typinggame.service;

import com.typinggame.dto.game.GameAdjustParam;
import com.typinggame.dto.game.GamePlayerPerformanceDTO;
import com.typinggame.entity.game.MonsterData;

import java.util.List;

/**
 * 处理遗传进化算法相关逻辑
 */
public interface EvolutionService {
    /**
     * 选择父本
     * @param userId 用户ID
     * @param monsters 怪物列表
     * @return 选择的父本列表
     */
    List<MonsterData> selectParents(Integer userId, List<MonsterData> monsters);

    /**
     * 基因重组与变异
     * @param parentStrings 父本字符串列表
     * @param errorChars 错误字符列表
     * @return 后代字符串列表
     */
    List<String> recombineAndMutate(List<String> parentStrings, List<String> errorChars);

    /**
     * 生成下一轮单词（按当前模式字符类型配置构建字符池）
     * @param userId 用户ID
     * @param roundNum 轮次
     * @param wordCount 单词数量
     * @return 下一轮单词列表
     */
    List<String> generateNextRoundWords(Integer userId, int roundNum, int wordCount);

    /**
     * 生成下一轮单词（可传入外部词源池；可指定 gameMode 以按该模式字符类型配置构建字符池）
     * @param userId 用户ID
     * @param roundNum 轮次
     * @param wordCount 单词数量
     * @param externalWordPool 外部词源列表（可为空，为空时与 generateNextRoundWords 行为一致）
     * @return 下一轮单词列表
     */
    List<String> generateNextRoundWords(Integer userId, int roundNum, int wordCount, List<String> externalWordPool);

    /**
     * 生成下一轮单词（按指定模式字符类型配置构建字符池，用于推荐预览词等）
     * @param userId 用户ID
     * @param roundNum 轮次
     * @param wordCount 单词数量
     * @param gameMode 前端模式字符串 endless/dictionary/limit，为空时按 endless
     * @return 下一轮单词列表
     */
    List<String> generateNextRoundWords(Integer userId, int roundNum, int wordCount, String gameMode);

    /**
     * 生成下一轮单词（指定 gameMode + 可选外部词源池）
     */
    List<String> generateNextRoundWords(Integer userId, int roundNum, int wordCount, String gameMode, List<String> externalWordPool);

    /**
     * 根据表现调整难度
     * @param userId 用户ID
     * @param performance 游戏表现数据
     * @return 调整后的难度参数
     */
    double adjustDifficulty(Integer userId, GamePlayerPerformanceDTO performance);

    /**
     * 设置进化策略（策略模式）
     * @param strategy 进化策略
     */
    void setEvolutionStrategy(String strategy);

    /**
     * 克隆怪物（原型模式）
     * @param monster 怪物数据
     * @return 克隆的怪物数据
     */
    MonsterData cloneMonster(MonsterData monster);

    /**
     * 计算父本分数
     * @param monster 怪物数据
     * @return 父本分数
     */
    double calculateParentScore(MonsterData monster);

    /**
     * 获取所有可用的进化策略
     * @return 进化策略列表
     */
    List<String> getEvolutionStrategies();

    /**
     * 保存父本字符串
     * @param userId 用户ID
     * @param parents 父本列表
     */
    void saveParentStrings(Integer userId, List<MonsterData> parents);

    /**
     * 根据数学模型输出参数生成下一轮怪物集（应用 Vₜ、Nₜ、Lₜ、类型占比，速度与长度受系统配置约束）
     * @param userId 用户ID
     * @param roundNum 当前轮次 K
     * @param gameMode 前端模式字符串：endless/dictionary/limit
     * @param adjustParam 本轮调节参数（含速度、数量、平均长度、类型占比）
     * @return 未持久化的怪物列表，供前端或写盘使用
     */
    List<MonsterData> generateNextRoundMonsters(Integer userId, int roundNum, String gameMode, GameAdjustParam adjustParam);

    /**
     * 生成下一轮怪物集（可传入外部词源，词源界定模式下优先使用词库词）
     * @param userId 用户ID
     * @param roundNum 当前轮次 K
     * @param gameMode 前端模式字符串：endless/dictionary/limit
     * @param adjustParam 本轮调节参数
     * @param externalWordPool 外部词源列表（可为空；非空时先从中取 Nₜ 个词作为 targetStr，不足再自动生成）
     * @return 未持久化的怪物列表
     */
    List<MonsterData> generateNextRoundMonsters(Integer userId, int roundNum, String gameMode, GameAdjustParam adjustParam, List<String> externalWordPool);
}
