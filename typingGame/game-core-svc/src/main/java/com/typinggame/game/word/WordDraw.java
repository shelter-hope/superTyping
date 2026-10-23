package com.typinggame.game.word;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class WordDraw {
    private WordDraw() {
    }

    public record Entry(String word, int weight) {
    }

    public static List<Integer> activeOnly(Collection<Integer> requested, Collection<Integer> activeIds) {
        if (requested == null || requested.isEmpty() || activeIds == null || activeIds.isEmpty()) {
            return List.of();
        }
        Set<Integer> active = new HashSet<>(activeIds);
        List<Integer> kept = new ArrayList<>();
        for (Integer id : requested) {
            if (id != null && active.contains(id)) kept.add(id);
        }
        return kept;
    }

    public static List<String> draw(List<Entry> pool, int count, Random random) {
        List<Entry> remaining = new ArrayList<>();
        if (pool != null) {
            for (Entry entry : pool) {
                if (entry == null || entry.word() == null || entry.word().isBlank()) continue;
                remaining.add(new Entry(entry.word().trim(), Math.max(1, Math.min(10, entry.weight()))));
            }
        }
        int safeCount = Math.max(3, Math.min(25, count));
        List<String> result = new ArrayList<>();
        while (result.size() < safeCount && !remaining.isEmpty()) {
            int total = remaining.stream().mapToInt(Entry::weight).sum();
            int pick = random.nextInt(total);
            int cursor = 0;
            int index = remaining.size() - 1;
            for (int i = 0; i < remaining.size(); i++) {
                cursor += remaining.get(i).weight();
                if (pick < cursor) {
                    index = i;
                    break;
                }
            }
            result.add(remaining.remove(index).word());
        }
        while (result.size() < safeCount && !result.isEmpty()) {
            result.add(result.get(random.nextInt(result.size())));
        }
        return result;
    }
}
