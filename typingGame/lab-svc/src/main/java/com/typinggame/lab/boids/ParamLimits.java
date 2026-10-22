package com.typinggame.lab.boids;

import java.util.Map;

public final class ParamLimits {
    private ParamLimits() {
    }

    public static void check(String algorithmId, Map<String, ?> params) {
        switch (algorithmId) {
            case "boids" -> BoidsLimits.check(params);
            case "pso" -> checkPso(params);
            case "ant-colony" -> checkAco(params);
            case "genetic" -> checkGenetic(params);
            default -> throw new IllegalArgumentException("暂不支持该算法的参数校验");
        }
    }

    private static void checkPso(Map<String, ?> params) {
        requireInt(params, "count", 1, 2000);
        require(params, "inertia", 0.1, 1.2);
        require(params, "cognitive", 0, 3);
        require(params, "social", 0, 3);
        require(params, "maxSpeed", 0.2, 8);
    }

    private static void checkAco(Map<String, ?> params) {
        requireInt(params, "ants", 1, 500);
        require(params, "alpha", 0, 4);
        require(params, "beta", 0, 6);
        require(params, "evaporation", 0.01, 0.8);
        require(params, "deposit", 0.1, 10);
    }

    private static void checkGenetic(Map<String, ?> params) {
        requireInt(params, "population", 2, 500);
        double population = number(params, "population");
        if (((int) population) % 2 != 0) {
            throw new IllegalArgumentException("population 必须是偶数");
        }
        require(params, "mutationRate", 0, 0.5);
        require(params, "crossoverRate", 0, 1);
        requireInt(params, "elitism", 0, 10);
    }

    static void requireInt(Map<String, ?> params, String key, int min, int max) {
        double value = number(params, key);
        if (value != Math.rint(value)) {
            throw new IllegalArgumentException(key + " 必须是整数");
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(key + " 超出区间 [" + min + ", " + max + "]");
        }
    }

    static void require(Map<String, ?> params, String key, double min, double max) {
        double value = number(params, key);
        if (value < min || value > max) {
            throw new IllegalArgumentException(key + " 超出区间 [" + min + ", " + max + "]");
        }
    }

    private static double number(Map<String, ?> params, String key) {
        if (params == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        Object value = params.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(key + " 必须是数字");
        }
        return number.doubleValue();
    }
}
