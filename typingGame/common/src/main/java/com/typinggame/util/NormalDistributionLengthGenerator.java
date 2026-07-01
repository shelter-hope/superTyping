package com.typinggame.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 生成符合正态分布的字符串长度数组工具类
 * 核心逻辑：基于中位数k（均值）生成正态分布数据，修正为正整数，保证长度在合理范围[1, 15]（匹配怪物字符长度约束）
 */
public class NormalDistributionLengthGenerator {

    // 正态分布的标准差系数（可配置，默认取中位数的1/4，保证数据集中在均值附近）
    private static final double DEFAULT_STD_DEV_RATIO = 0.25;
    // 字符串长度最小值（与system_monster_configs中的minStrLength一致）
    private static final int MIN_LENGTH = 1;
    // 字符串长度最大值（与system_monster_configs中的maxStrLength一致）
    private static final int MAX_LENGTH = 15;

    /**
     * 生成符合正态分布的字符串长度数组
     * @param median k，正态分布的中位数（均值）
     * @param count n，生成的数组元素数量
     * @return 正整数数组，元素为字符串长度，符合正态分布
     */
    public static int[] generateNormalDistLengths(int median, int count) {
        // 校验参数合法性
        if (median < MIN_LENGTH || median > MAX_LENGTH) {
            throw new IllegalArgumentException("中位数k需在[" + MIN_LENGTH + "," + MAX_LENGTH + "]范围内");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("数量值n必须大于0");
        }

        Random random = new Random();
        List<Integer> lengths = new ArrayList<>();
        // 正态分布的标准差 = 中位数 * 标准差系数（可根据需求调整）
        double stdDev = median * DEFAULT_STD_DEV_RATIO;

        // 生成正态分布数据并修正为合法长度
        while (lengths.size() < count) {
            // nextGaussian()生成均值为0、标准差为1的正态分布随机数
            double normalValue = random.nextGaussian() * stdDev + median;
            // 四舍五入为整数
            int length = (int) Math.round(normalValue);
            // 修正到[MIN_LENGTH, MAX_LENGTH]范围内
            length = Math.max(MIN_LENGTH, Math.min(length, MAX_LENGTH));
            lengths.add(length);
        }

        // 转换为数组返回
        return lengths.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 重载方法：支持自定义标准差（管理员可配置）
     * @param median 中位数k
     * @param count 数量n
     * @param stdDev 自定义标准差
     * @return 字符串长度数组
     */
    public static int[] generateNormalDistLengths(int median, int count, double stdDev) {
        if (median < MIN_LENGTH || median > MAX_LENGTH) {
            throw new IllegalArgumentException("中位数k需在[" + MIN_LENGTH + "," + MAX_LENGTH + "]范围内");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("数量值n必须大于0");
        }
        if (stdDev <= 0) {
            throw new IllegalArgumentException("标准差必须大于0");
        }

        Random random = new Random();
        List<Integer> lengths = new ArrayList<>();

        while (lengths.size() < count) {
            double normalValue = random.nextGaussian() * stdDev + median;
            int length = (int) Math.round(normalValue);
            length = Math.max(MIN_LENGTH, Math.min(length, MAX_LENGTH));
            lengths.add(length);
        }

        return lengths.stream().mapToInt(Integer::intValue).toArray();
    }

    // 测试示例
    public static void main(String[] args) {
        // 示例：中位数k=5，数量n=10，生成字符串长度数组
        int[] lengths = generateNormalDistLengths(5, 10);
        System.out.println("生成的字符串长度数组：");
        for (int len : lengths) {
            System.out.print(len + " ");
        }
        // 输出示例：5 4 6 5 7 5 4 5 6 5（集中在5附近，符合正态分布）
    }
}