package com.typinggame.lab.boids;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParamLimitsTest {

    @Test
    void acceptsPluginDefaultsAndRejectsOutOfRange() {
        assertDoesNotThrow(() -> ParamLimits.check("pso", map("count", 80, "inertia", 0.7, "cognitive", 1.4, "social", 1.4, "maxSpeed", 3)));
        assertThrows(IllegalArgumentException.class, () -> ParamLimits.check("pso", map("count", 0, "inertia", 0.7, "cognitive", 1, "social", 1, "maxSpeed", 3)));
        assertDoesNotThrow(() -> ParamLimits.check("ant-colony", map("ants", 40, "alpha", 1, "beta", 2, "evaporation", 0.2, "deposit", 2)));
        assertThrows(IllegalArgumentException.class, () -> ParamLimits.check("genetic", map("population", 61, "mutationRate", 0.05, "crossoverRate", 0.8, "elitism", 2)));
        assertDoesNotThrow(() -> ParamLimits.check("genetic", map("population", 60, "mutationRate", 0.05, "crossoverRate", 0.8, "elitism", 2)));
        assertThrows(IllegalArgumentException.class, () -> ParamLimits.check("unknown", map("count", 1)));
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> params = new HashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            params.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return params;
    }
}
