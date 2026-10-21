package com.typinggame.admin.controller;

/**
 * 后台接口路径 -> 下游服务的路由表
 */
final class AdminRoutes {

    private AdminRoutes() {
    }

    static String resolveTargetService(String path) {
        if (path.matches("/users/\\d+/game-data")) {
            return "game-core-svc";
        }
        if (path.startsWith("/users")
                || path.startsWith("/friend-relations")
                || path.startsWith("/user-app-configs")
                || path.startsWith("/user-game-mode-configs")
                || path.startsWith("/user-monster-configs")
                || path.startsWith("/user-model-params")
                || path.startsWith("/user-daily-stats")
                || path.startsWith("/user-info-snapshots")) {
            return "user-svc";
        }
        if (path.startsWith("/system-dictionaries")
                || path.startsWith("/system-dictionary-words")
                || path.startsWith("/custom-dictionaries")
                || path.startsWith("/custom-dictionary-words")
                || path.startsWith("/leaderboard-entries")
                || path.startsWith("/user-conversations")
                || path.startsWith("/friend-chat-messages")
                || path.startsWith("/system-notifications")) {
            return "chat-svc";
        }
        if (path.startsWith("/ai-conversations") || path.startsWith("/ai-chat-messages")) {
            return "ai-svc";
        }
        return "game-core-svc";
    }

    static String contextPath(String service) {
        return switch (service) {
            case "user-svc" -> "/user-svc";
            case "game-core-svc" -> "/game-core-svc";
            case "chat-svc" -> "/chat-svc";
            case "ai-svc" -> "/ai-svc";
            default -> throw new IllegalArgumentException("不支持的后台目标服务: " + service);
        };
    }
}
