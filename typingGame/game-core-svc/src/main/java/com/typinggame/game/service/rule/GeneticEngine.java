package com.typinggame.game.service.rule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 字符串基因引擎：交叉、变异、锦标赛选择与精英保留。
 *
 * <p>v2 起的行为约定：
 * <ul>
 *   <li>每个字符位置都从父本按位取值，子代长度严格等于目标长度（v1 用「把自己第 0 位反复追加」补齐，会把同一个错字放大成整段）。</li>
 *   <li>交叉支持单点、双点与均匀三种模式，由 {@code GeneticRule.crossover} 决定。</li>
 *   <li>变异优先按「混淆对」定向替换（真实打错的那个键），其次才退回易错字符池，最后才用字母表兜底 —— v1 固定取「下一个字符」，等于只制造位置相关的错字。</li>
 *   <li>选择按适应度做锦标赛，最好的父本按 {@code eliteRatio} 原样保留，避免每代都把最优解拆掉。</li>
 *   <li>随机源由调用方注入，给定种子即可复现一整局的进化过程，便于审计与测试。</li>
 * </ul>
 */
public final class GeneticEngine {

    public static final String CROSSOVER_SINGLE_POINT = "SINGLE_POINT";
    public static final String CROSSOVER_TWO_POINT = "TWO_POINT";
    public static final String CROSSOVER_UNIFORM = "UNIFORM";

    private static final String FALLBACK_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private GeneticEngine() {
    }

    /** 带适应度的父本。fitness 越大越容易被选中，也越可能被原样保留。 */
    public record Parent(String text, double fitness) {
    }

    /** 一次进化请求。count / length 是目标数量与目标长度。 */
    public record Request(List<Parent> parents,
                          List<String> weakChars,
                          Map<String, List<String>> confusion,
                          int count,
                          int length,
                          double mutationRate,
                          double eliteRatio,
                          int tournamentSize,
                          String crossoverMode,
                          Random random) {
    }

    public record Mutation(String text, List<Integer> positions, List<String> chars) {
    }

    public static List<String> evolve(Request request) {
        Random random = request.random() == null ? new Random() : request.random();
        List<Parent> pool = request.parents() == null ? List.of() : request.parents().stream()
                .filter(parent -> parent != null && parent.text() != null && !parent.text().isBlank())
                .sorted(Comparator.comparingDouble(Parent::fitness).reversed())
                .toList();
        if (pool.isEmpty()) {
            return List.of();
        }
        int count = Math.max(1, request.count());
        int length = Math.max(1, request.length());
        Set<String> result = new LinkedHashSet<>();

        int eliteCount = (int) Math.round(clamp(request.eliteRatio(), 0, 1) * count);
        eliteCount = Math.min(eliteCount, Math.min(pool.size(), count));
        for (int i = 0; i < eliteCount; i++) {
            result.add(fit(pool.get(i).text(), length));
        }

        int tournamentSize = Math.max(1, request.tournamentSize());
        int attempts = 0;
        int maxAttempts = Math.max(16, count * 12);
        while (result.size() < count && attempts++ < maxAttempts) {
            String first = tournament(pool, tournamentSize, random).text();
            String second = tournament(pool, tournamentSize, random).text();
            String child = crossover(first, second, length, request.crossoverMode(), random);
            child = mutate(child, request.weakChars(), request.confusion(), request.mutationRate(), random).text();
            result.add(child);
        }
        return new ArrayList<>(result);
    }

    /** v1 兼容入口：没有适应度、没有混淆对时按单点交叉与易错字符变异处理。 */
    public static List<String> evolve(List<String> parents, List<String> weakChars, int count, int length,
                                      double mutationRate, Random random) {
        List<Parent> parentList = parents == null ? List.of()
                : parents.stream().map(text -> new Parent(text, 0)).toList();
        return evolve(new Request(parentList, weakChars, Map.of(), count, length, mutationRate,
                0, 3, CROSSOVER_SINGLE_POINT, random));
    }

    public static String crossover(String first, String second, int length, Random random) {
        return crossover(first, second, length, CROSSOVER_SINGLE_POINT, random);
    }

    public static String crossover(String first, String second, int length, String mode, Random random) {
        String left = normalize(first);
        String right = normalize(second);
        if (left.isEmpty() && right.isEmpty()) {
            return "";
        }
        if (left.isEmpty()) {
            left = right;
        }
        if (right.isEmpty()) {
            right = left;
        }
        int targetLength = Math.max(1, length);
        String normalizedMode = mode == null ? CROSSOVER_SINGLE_POINT : mode.trim().toUpperCase();
        StringBuilder child = new StringBuilder(targetLength);
        switch (normalizedMode) {
            case CROSSOVER_TWO_POINT -> {
                int firstCut = random.nextInt(targetLength + 1);
                int secondCut = firstCut + random.nextInt(targetLength - firstCut + 1);
                for (int i = 0; i < targetLength; i++) {
                    boolean fromSecond = i >= firstCut && i < secondCut;
                    child.append(charAt(fromSecond ? right : left, i));
                }
            }
            case CROSSOVER_UNIFORM -> {
                for (int i = 0; i < targetLength; i++) {
                    child.append(charAt(random.nextBoolean() ? right : left, i));
                }
            }
            default -> {
                int cut = random.nextInt(targetLength + 1);
                for (int i = 0; i < targetLength; i++) {
                    child.append(charAt(i < cut ? left : right, i));
                }
            }
        }
        return child.toString();
    }

    public static Mutation mutate(String value, List<String> weakChars, double rate, boolean preferWeak, Random random) {
        return mutate(value, preferWeak ? weakChars : List.of(), Map.of(), rate, random);
    }

    public static Mutation mutate(String value, List<String> weakChars, Map<String, List<String>> confusion,
                                  double rate, Random random) {
        String text = value == null ? "" : value;
        if (text.isEmpty() || random.nextDouble() >= rate) {
            return new Mutation(text, List.of(), List.of());
        }
        char[] chars = text.toCharArray();
        int position = random.nextInt(chars.length);
        String replacement = chooseReplacement(String.valueOf(chars[position]), weakChars, confusion, random);
        chars[position] = replacement.charAt(0);
        return new Mutation(new String(chars), List.of(position), List.of(replacement));
    }

    public static <T> List<T> limitParents(List<T> parents, int limit) {
        if (parents == null || parents.isEmpty()) return List.of();
        int size = Math.max(1, limit);
        return parents.size() <= size ? parents : parents.subList(0, size);
    }

    /** 锦标赛选择：随机抽 size 个候选，取适应度最高的一个。size<=1 时等价于均匀随机。 */
    public static <T> T tournamentPick(List<T> pool, java.util.function.ToDoubleFunction<T> fitness,
                                       int size, Random random) {
        if (pool == null || pool.isEmpty()) return null;
        T best = pool.get(random.nextInt(pool.size()));
        for (int i = 1; i < Math.max(1, size); i++) {
            T candidate = pool.get(random.nextInt(pool.size()));
            if (fitness.applyAsDouble(candidate) > fitness.applyAsDouble(best)) {
                best = candidate;
            }
        }
        return best;
    }

    private static Parent tournament(List<Parent> pool, int size, Random random) {
        Parent best = null;
        for (int i = 0; i < size; i++) {
            Parent candidate = pool.get(random.nextInt(pool.size()));
            if (best == null || candidate.fitness() > best.fitness()) {
                best = candidate;
            }
        }
        return best == null ? pool.get(0) : best;
    }

    private static String chooseReplacement(String source, List<String> weakChars,
                                            Map<String, List<String>> confusion, Random random) {
        List<String> confused = confusion == null ? null : confusion.get(source);
        if (confused != null && !confused.isEmpty()) {
            return pick(confused, random);
        }
        if (weakChars != null && !weakChars.isEmpty()) {
            return pick(weakChars, random);
        }
        for (int attempt = 0; attempt < FALLBACK_ALPHABET.length(); attempt++) {
            char candidate = FALLBACK_ALPHABET.charAt(random.nextInt(FALLBACK_ALPHABET.length()));
            if (candidate != source.charAt(0)) {
                return String.valueOf(candidate);
            }
        }
        return String.valueOf(source.charAt(0) == 'a' ? 'b' : 'a');
    }

    private static String pick(List<String> values, Random random) {
        for (int attempt = 0; attempt < values.size(); attempt++) {
            String value = values.get(random.nextInt(values.size()));
            if (value != null && !value.isEmpty()) {
                return value.substring(0, 1);
            }
        }
        String fallback = values.get(0);
        return fallback == null || fallback.isEmpty() ? "a" : fallback.substring(0, 1);
    }

    /** 按位取字符，越界则回绕；保证任意父本长度都能拼出目标长度。 */
    private static char charAt(String text, int index) {
        return text.charAt(index % text.length());
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    private static String fit(String text, int length) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.length() >= length) {
            return normalized.substring(0, length);
        }
        StringBuilder builder = new StringBuilder(normalized);
        while (builder.length() < length) {
            builder.append(charAt(normalized, builder.length()));
        }
        return builder.toString();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
