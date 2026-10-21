package com.typinggame.lab.boids;

import java.util.Map;

public final class BoidsLimits {
    private BoidsLimits() {
    }

    public static void check(Map<String, ?> params) {
        if (params == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        requireInt(params, "count", 1, 2000);
        require(params, "visionRadius", 1, 200);
        require(params, "visionAngle", 0.2, Math.PI * 2);
        require(params, "maxSpeed", 0.2, 8);
        require(params, "maxForce", 0.005, 0.5);
        require(params, "separation", 0, 4);
        require(params, "alignment", 0, 4);
        require(params, "cohesion", 0, 4);
    }

    private static void requireInt(Map<String, ?> params, String key, int min, int max) {
        double value = number(params, key);
        if (value != Math.rint(value)) {
            throw new IllegalArgumentException(key + " 必须是整数");
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(key + " 超出区间 [" + min + ", " + max + "]");
        }
    }

    private static void require(Map<String, ?> params, String key, double min, double max) {
        double value = number(params, key);
        if (value < min || value > max) {
            throw new IllegalArgumentException(key + " 超出区间 [" + min + ", " + max + "]");
        }
    }

    private static double number(Map<String, ?> params, String key) {
        Object value = params.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(key + " 必须是数字");
        }
        return number.doubleValue();
    }
}
