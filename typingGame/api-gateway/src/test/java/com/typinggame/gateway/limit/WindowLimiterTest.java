package com.typinggame.gateway.limit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowLimiterTest {

    @Test
    void blocksAfterTheWindowIsFullAndOpensOnTheNextWindow() {
        WindowLimiter limiter = new WindowLimiter(2, 1000);
        assertTrue(limiter.allow("ip:1", 0));
        assertTrue(limiter.allow("ip:1", 10));
        assertFalse(limiter.allow("ip:1", 20));
        assertTrue(limiter.allow("user:9", 20));
        assertTrue(limiter.allow("ip:1", 1000));
    }
}
