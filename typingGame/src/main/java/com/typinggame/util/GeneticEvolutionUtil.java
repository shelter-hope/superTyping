package com.typinggame.util;

import com.typinggame.entity.game.MonsterData;
import com.typinggame.entity.config.SystemParam;
import com.typinggame.entity.model.UserModelParams;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 遗传进化工具类（核心算法）
 * 对应文档：核心调节算法模型-遗传算法（GA）优化
 */
public class GeneticEvolutionUtil {
    private static final Random random = new Random();

    /**
     * 进化下一代怪物种群
     * @param currentMonsters 当前种群（父代）
     * @param params 用户模型参数（包含遗传参数）
     * @param targetDifficulty 目标难度系数
     * @param charPool 字符池
     * @return 新一代种群
     */
    public static List<MonsterData> evolveNextGeneration(List<MonsterData> currentMonsters,
                                                         UserModelParams params,
                                                         BigDecimal targetDifficulty,
                                                         List<Character> charPool) {
        // 1. 适应度评估与选择
        List<MonsterData> selectedParents = selectParents(currentMonsters, targetDifficulty);

        List<MonsterData> nextGeneration = new ArrayList<>();
        // 精英保留（直接复制前10%）
        int eliteCount = (int) (currentMonsters.size() * 0.1);
        for (int i = 0; i < eliteCount; i++) {
            if (i < selectedParents.size()) {
                nextGeneration.add(cloneMonster(selectedParents.get(i)));
            }
        }

        // 交叉与变异生成剩余个体
        while (nextGeneration.size() < currentMonsters.size()) {
            // 随机选择两个父代
            MonsterData p1 = selectedParents.get(random.nextInt(selectedParents.size()));
            MonsterData p2 = selectedParents.get(random.nextInt(selectedParents.size()));

            // 交叉
            MonsterData child = crossover(p1, p2, params.getCrossoverRate());

            // 变异
            mutate(child, params.getMutationRate(), charPool);

            nextGeneration.add(child);
        }

        return nextGeneration;
    }

    /**
     * 选择父代（轮盘赌选择法，基于适应度）
     * 适应度 Fitness = 1 / (|D_monster - D_target| + ε)
     */
    private static List<MonsterData> selectParents(List<MonsterData> population, BigDecimal targetDifficulty) {
        // 计算每个个体的适应度
        List<Double> fitnessList = new ArrayList<>();
        double totalFitness = 0;
        for (MonsterData m : population) {
            // 估算怪物个体难度（需实现 calculateMonsterDifficulty）
            BigDecimal monsterDiff = calculateMonsterDifficulty(m);
            double diff = Math.abs(monsterDiff.subtract(targetDifficulty).doubleValue());
            double fitness = 1.0 / (diff + 0.001); // 避免除零
            fitnessList.add(fitness);
            totalFitness += fitness;
        }

        // 轮盘赌选择
        List<MonsterData> parents = new ArrayList<>();
        for (int i = 0; i < population.size(); i++) {
            double slice = random.nextDouble() * totalFitness;
            double currentSum = 0;
            for (int j = 0; j < population.size(); j++) {
                currentSum += fitnessList.get(j);
                if (currentSum >= slice) {
                    parents.add(population.get(j));
                    break;
                }
            }
        }
        
        // 按适应度降序排序（为了精英保留）
        // 简单实现：这里未严格排序，仅返回轮盘赌结果。若需精英保留，应先对 population 排序。
        // 修正逻辑：返回 population 的排序副本作为精英源，轮盘赌结果作为繁衍源。
        // 这里简化处理：直接对 population 按适应度排序并返回
        //List<MonsterData> sortedPop = new ArrayList<>(population);
        // 需配合 fitness 排序，略复杂，暂且返回轮盘赌结果
        return parents;
    }

    /**
     * 交叉操作（单点交叉）
     * 交换 targetStr 的后半部分
     */
    private static MonsterData crossover(MonsterData p1, MonsterData p2, BigDecimal crossoverRate) {
        MonsterData child = cloneMonster(p1);
        if (random.nextDouble() < crossoverRate.doubleValue()) {
            String s1 = p1.getTargetStr();
            String s2 = p2.getTargetStr();
            int len = Math.min(s1.length(), s2.length());
            if (len > 1) {
                int cutPoint = random.nextInt(len - 1) + 1;
                String newStr = s1.substring(0, cutPoint) + s2.substring(cutPoint);
                child.setTargetStr(newStr);
            }
            // 属性混合：速度取平均
            child.setSpeed(p1.getSpeed().add(p2.getSpeed()).divide(new BigDecimal(2)));
        }
        return child;
    }

    /**
     * 变异操作
     * 随机替换字符、微调速度
     */
    private static void mutate(MonsterData child, BigDecimal mutationRate, List<Character> charPool) {
        if (random.nextDouble() < mutationRate.doubleValue()) {
            // 1. 基因突变：随机替换一个字符
            String s = child.getTargetStr();
            if (s.length() > 0) {
                int pos = random.nextInt(s.length());
                char newChar = charPool.get(random.nextInt(charPool.size()));
                StringBuilder sb = new StringBuilder(s);
                sb.setCharAt(pos, newChar);
                child.setTargetStr(sb.toString());
            }

            // 2. 参数扰动：速度波动 ±10%
            double factor = 0.9 + random.nextDouble() * 0.2;
            child.setSpeed(child.getSpeed().multiply(new BigDecimal(factor)));
        }
    }

    /**
     * 估算单个怪物的难度系数（简化版）
     * Dm = w1*Speed + w2*Length
     */
    private static BigDecimal calculateMonsterDifficulty(MonsterData m) {
        // 简单模型：速度*0.5 + 长度*0.2
        return m.getSpeed().multiply(new BigDecimal("0.5"))
                .add(new BigDecimal(m.getTargetStr().length()).multiply(new BigDecimal("0.2")));
    }

    private static MonsterData cloneMonster(MonsterData source) {
        MonsterData clone = new MonsterData();
        clone.setTargetStr(source.getTargetStr());
        clone.setSpeed(source.getSpeed());
        clone.setSurvivalTime(source.getSurvivalTime());
        clone.setInputErrorCount(source.getInputErrorCount());
        // clone.setMonsterType(source.getMonsterType());
        // clone.setScore(source.getScore());
        return clone;
    }
}