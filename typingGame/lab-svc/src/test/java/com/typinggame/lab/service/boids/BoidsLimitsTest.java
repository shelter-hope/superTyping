package com.typinggame.lab.service.boids;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoidsLimitsTest {

    @Test
    void acceptsDefaultRange() {
        assertDoesNotThrow(() -> BoidsLimits.check(defaults()));
    }

    @Test
    void rejectsCountOutsideRangeAndFractions() {
        Map<String, Object> low = defaults();
        low.put("count", 0);
        assertThrows(IllegalArgumentException.class, () -> BoidsLimits.check(low));

        Map<String, Object> high = defaults();
        high.put("count", 2001);
        assertThrows(IllegalArgumentException.class, () -> BoidsLimits.check(high));

        Map<String, Object> fraction = defaults();
        fraction.put("count", 1.5);
        assertThrows(IllegalArgumentException.class, () -> BoidsLimits.check(fraction));
    }

    @Test
    void rejectsNarrowVisionAndMissingKeys() {
        Map<String, Object> angle = defaults();
        angle.put("visionAngle", 0.1);
        assertThrows(IllegalArgumentException.class, () -> BoidsLimits.check(angle));

        Map<String, Object> missing = defaults();
        missing.remove("cohesion");
        assertThrows(IllegalArgumentException.class, () -> BoidsLimits.check(missing));
    }

    private static Map<String, Object> defaults() {
        Map<String, Object> params = new HashMap<>();
        params.put("count", 80);
        params.put("visionRadius", 40);
        params.put("visionAngle", Math.PI);
        params.put("maxSpeed", 2.2);
        params.put("maxForce", 0.05);
        params.put("separation", 1.4);
        params.put("alignment", 0.8);
        params.put("cohesion", 0.6);
        return params;
    }
}
