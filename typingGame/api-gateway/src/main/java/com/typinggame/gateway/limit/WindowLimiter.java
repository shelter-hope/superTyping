package com.typinggame.gateway.limit;

import java.util.concurrent.ConcurrentHashMap;

public final class WindowLimiter {
    private final int limit;
    private final long windowMillis;
    private final ConcurrentHashMap<String, long[]> windows = new ConcurrentHashMap<>();

    public WindowLimiter(int limit, long windowMillis) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        this.limit = limit;
        this.windowMillis = windowMillis;
    }

    public boolean allow(String key, long nowMillis) {
        long[] state = windows.compute(key, (ignored, current) -> {
            if (current == null || nowMillis - current[0] >= windowMillis) {
                return new long[]{nowMillis, 1};
            }
            current[1] += 1;
            return current;
        });
        return state[1] <= limit;
    }
}
