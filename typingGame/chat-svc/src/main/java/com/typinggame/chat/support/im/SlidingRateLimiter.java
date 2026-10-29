package com.typinggame.chat.support.im;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingRateLimiter {
    private final int limit;
    private final long windowMillis;
    private final Map<Integer, Deque<Long>> hits = new ConcurrentHashMap<>();

    public SlidingRateLimiter(int limit, long windowMillis) {
        this.limit = limit;
        this.windowMillis = windowMillis;
    }

    public boolean allow(Integer userId, long now) {
        Deque<Long> window = hits.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= windowMillis) {
                window.removeFirst();
            }
            if (window.size() >= limit) return false;
            window.addLast(now);
            return true;
        }
    }
}
