package com.typinggame.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 在旧的平均数下，求新的平均速
 */
public class AvgMathUtil {
    /**
     * 动态追加单个数据，快速计算新平均值
     * @param oldAvg  旧平均值
     * @param oldCount 旧数据总个数
     * @param newData  新增数据
     * @return 新平均值
     */
    public static BigDecimal calcNewAvg(BigDecimal oldAvg, int oldCount, BigDecimal newData) {
        // 核心公式直接套用，一步计算 (oldAvg * oldCount + newData) / (oldCount + 1)
        BigDecimal oldSum = oldAvg.multiply(new BigDecimal(oldCount));
        BigDecimal newSum = oldSum.add(newData);
        return newSum.divide(new BigDecimal(oldCount + 1), 2, RoundingMode.HALF_UP);
    }

    /**
     * 批量追加多个数据，快速计算新平均值
     * @param oldAvg  旧平均值
     * @param oldCount 旧数据总个数
     * @param newDataList 新增的数据集
     * @return 新平均值
     */
    public static BigDecimal calcNewAvgByBatch(BigDecimal oldAvg, int oldCount, BigDecimal[] newDataList) {
        if (newDataList == null || newDataList.length == 0) {
            return oldAvg; // 无新数据，返回原平均值
        }
        // 计算新增数据总和
        BigDecimal newDataSum = new BigDecimal(0);
        for (BigDecimal num : newDataList) {
            newDataSum = newDataSum.add(num);
        }
        int newCount = newDataList.length;

        BigDecimal oldSum = oldAvg.multiply(new BigDecimal(oldCount));
        BigDecimal newSum = oldSum.add(newDataSum);

        // 批量扩展公式
        return newSum.divide (new BigDecimal(oldCount + newCount),2,RoundingMode.HALF_UP);
    }
}
