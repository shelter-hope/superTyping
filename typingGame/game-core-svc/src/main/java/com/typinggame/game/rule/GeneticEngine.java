package com.typinggame.game.rule;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class GeneticEngine {
    private GeneticEngine() {
    }

    public static String crossover(String first, String second, int length, Random random) {
        String left = first == null ? "" : first;
        String right = second == null ? "" : second;
        int cut = left.isEmpty() ? 0 : random.nextInt(left.length() + 1);
        String child = left.substring(0, cut) + right.substring(Math.min(cut, right.length()));
        if (child.isEmpty()) {
            child = left.isEmpty() ? right : left;
        }
        if (child.isEmpty()) {
            child = "a";
        }
        StringBuilder normalized = new StringBuilder(child);
        while (normalized.length() < length) {
            normalized.append(normalized.charAt(normalized.length() % normalized.length()));
        }
        return normalized.substring(0, Math.min(length, normalized.length()));
    }

    public static Mutation mutate(String value, List<String> weakChars, double rate, boolean preferWeak, Random random) {
        if (value == null || value.isEmpty() || random.nextDouble() >= rate) {
            return new Mutation(value == null ? "" : value, List.of(), List.of());
        }
        char[] chars = value.toCharArray();
        int position = random.nextInt(chars.length);
        String replacement = choose(value, position, weakChars, preferWeak, random);
        chars[position] = replacement.charAt(0);
        return new Mutation(new String(chars), List.of(position), List.of(replacement));
    }

    public static List<String> evolve(List<String> parents, List<String> weakChars, int count, int length, double mutationRate, Random random) {
        List<String> pool = parents == null ? List.of() : parents.stream().filter(item -> item != null && !item.isBlank()).toList();
        if (pool.isEmpty()) {
            return List.of();
        }
        int safeCount = Math.max(1, count);
        int safeLength = Math.max(1, length);
        Set<String> result = new LinkedHashSet<>();
        int attempts = 0;
        while (result.size() < safeCount && attempts++ < safeCount * 8) {
            String first = pool.get(random.nextInt(pool.size()));
            String second = pool.get(random.nextInt(pool.size()));
            String child = crossover(first, second, safeLength, random);
            child = mutate(child, weakChars, mutationRate, true, random).text();
            result.add(child);
        }
        return new ArrayList<>(result);
    }

    public static <T> List<T> limitParents(List<T> parents, int limit) {
        if (parents == null || parents.isEmpty()) return List.of();
        int size = Math.max(1, limit);
        return parents.size() <= size ? parents : parents.subList(0, size);
    }

    private static String choose(String text, int position, List<String> weakChars, boolean preferWeak, Random random) {
        if (preferWeak && weakChars != null && !weakChars.isEmpty()) {
            String weak = weakChars.get(random.nextInt(weakChars.size()));
            if (weak != null && !weak.isEmpty()) return weak.substring(0, 1);
        }
        return String.valueOf(text.charAt((position + 1) % text.length()));
    }

    public record Mutation(String text, List<Integer> positions, List<String> chars) {
    }
}
