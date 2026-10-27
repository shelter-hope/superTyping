package com.typinggame.admin.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 后台接口路径 -> 下游服务的显式路由表。
 * 精确正则优先于前缀；未命中时落到 game-core-svc。
 */
final class AdminRoutes {

    private static final List<Route> ROUTES = List.of(
            Route.regex("/users/\\d+/game-data", "game-core-svc"),
            Route.prefix("/users", "user-svc"),
            Route.prefix("/friend-relations", "user-svc"),
            Route.prefix("/user-app-configs", "user-svc"),
            Route.prefix("/user-game-mode-configs", "user-svc"),
            Route.prefix("/user-monster-configs", "user-svc"),
            Route.prefix("/user-model-params", "user-svc"),
            Route.prefix("/user-daily-stats", "user-svc"),
            Route.prefix("/user-info-snapshots", "user-svc"),
            Route.prefix("/system-dictionaries", "dict-svc"),
            Route.prefix("/system-dictionary-words", "dict-svc"),
            Route.prefix("/custom-dictionaries", "dict-svc"),
            Route.prefix("/custom-dictionary-words", "dict-svc"),
            Route.prefix("/leaderboard-entries", "game-core-svc"),
            Route.prefix("/key-confusion", "game-core-svc"),
            Route.prefix("/user-conversations", "chat-svc"),
            Route.prefix("/friend-chat-messages", "chat-svc"),
            Route.prefix("/system-notifications", "chat-svc"),
            Route.prefix("/ai-conversations", "ai-svc"),
            Route.prefix("/ai-chat-messages", "ai-svc"),
            Route.prefix("/lab", "lab-svc")
    );

    private static final Map<String, String> CONTEXT_PATHS = Map.of(
            "user-svc", "/user-svc",
            "game-core-svc", "/game-core-svc",
            "chat-svc", "/chat-svc",
            "ai-svc", "/ai-svc",
            "lab-svc", "/lab-svc",
            "dict-svc", "/dict-svc"
    );

    private AdminRoutes() {
    }

    static String resolveTargetService(String path) {
        for (Route route : ROUTES) {
            if (route.matches(path)) {
                return route.service();
            }
        }
        return "game-core-svc";
    }

    static String contextPath(String service) {
        String path = CONTEXT_PATHS.get(service);
        if (path == null) {
            throw new IllegalArgumentException("不支持的后台目标服务: " + service);
        }
        return path;
    }

    /** 测试/诊断用：当前路由表快照。 */
    static Map<String, String> routeTableSnapshot() {
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (Route route : ROUTES) {
            snapshot.put(route.pattern(), route.service());
        }
        return snapshot;
    }

    private record Route(String pattern, String service, boolean regex) {
        static Route prefix(String prefix, String service) {
            return new Route(prefix, service, false);
        }

        static Route regex(String regex, String service) {
            return new Route(regex, service, true);
        }

        boolean matches(String path) {
            if (path == null) {
                return false;
            }
            if (regex) {
                return Pattern.matches(pattern, path);
            }
            return path.startsWith(pattern);
        }
    }
}
