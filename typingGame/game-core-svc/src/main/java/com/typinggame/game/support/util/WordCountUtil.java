package com.typinggame.game.support.util;

import com.typinggame.game.entity.GameRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符/单词统计工具类（文本处理基础）
 * 对应文档：核心调节算法模型-数据标准化处理
 */
public class WordCountUtil {

    // 正则模式（支持中文、英文、数字、标点符号）
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]+|[\\u4e00-\\u9fa5]+|[0-9]+|[^\\s\\w]");

    /**
     * 计算实时速度 (St)
     * St = (N_correct / T_duration) * 60
     * @param correctChars 正确输入字符数
     * @param durationSeconds 游戏时长（秒）
     * @return 字符/分钟
     */
    public static BigDecimal calculateRealSpeed(int correctChars, double durationSeconds) {
        if (durationSeconds <= 0) return BigDecimal.ZERO;
        return new BigDecimal(correctChars)
                .divide(new BigDecimal(durationSeconds), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(60))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算实时准确率 (At)
     * At = N_correct / N_total
     * @param correctChars 正确输入字符数
     * @param totalInputChars 总输入字符数
     * @return 0.0 - 1.0
     */
    public static BigDecimal calculateRealAccuracy(int correctChars, int totalInputChars) {
        if (totalInputChars <= 0) return BigDecimal.ZERO;
        return new BigDecimal(correctChars)
                .divide(new BigDecimal(totalInputChars), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
    }

    /**
     * 计算易错字符频率 (Eit) - 基于 GameRecord
     * Eit = Σ(e_i * w_i) / Σ(c_i * w_i)
     * 简化版：错误字符总数 / 总输入字符数 (实际应加权)
     * @param record 游戏记录
     * @return 0.0 - 1.0
     */
    public static BigDecimal calculateCharErrorRate(GameRecord record) {
        if (record.getTotalChars() == null || record.getTotalChars() == 0) return BigDecimal.ZERO;
        int errorChars = record.getTotalChars() - record.getCorrectChars();
        return new BigDecimal(errorChars)
                .divide(new BigDecimal(record.getTotalChars()), 4, RoundingMode.HALF_UP);
    }

    /**
     * 解析文本并统计词频
     * @param text 输入文本
     * @return 单词 -> 频率 Map
     */
    public static Map<String, Integer> countWordFrequency(String text) {
        Map<String, Integer> frequencyMap = new HashMap<>();
        if (text == null || text.isEmpty()) return frequencyMap;

        Matcher matcher = WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            String word = matcher.group();
            frequencyMap.put(word, frequencyMap.getOrDefault(word, 0) + 1);
        }
        return frequencyMap;
    }
}
