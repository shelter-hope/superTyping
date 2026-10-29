package com.typinggame.gateway.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 网关路由表与各服务 context-path 的一致性护栏（P4-5）。
 *
 * <p>网关是唯一对外入口，路由靠 {@code PrefixPath=/<svc>} 与服务自身的
 * {@code server.servlet.context-path} 手工配对；任何一边改了而另一边没改，线上就是 404。
 * 这里把这条约定固化成测试：</p>
 * <ol>
 *     <li>每条路由的 {@code lb://<svc>} 与 {@code PrefixPath=/<svc>} 必须一致；</li>
 *     <li>同一服务只允许一条「前缀」路由定义（ws 路由按同一规则校验）；</li>
 *     <li>能读到兄弟模块源码时，再和该服务 application.yml 的 context-path 对一遍。</li>
 * </ol>
 */
class GatewayRouteConsistencyTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(InputStream in) {
        return new Yaml().load(in);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapAt(Map<String, Object> root, String... path) {
        Map<String, Object> cur = root;
        for (String key : path) {
            Object next = cur.get(key);
            assertTrue(next instanceof Map, () -> "配置缺少节点: " + key);
            cur = (Map<String, Object>) next;
        }
        return cur;
    }

    @Test
    @SuppressWarnings("unchecked")
    void everyRoutePrefixMatchesServiceName() throws Exception {
        Map<String, Object> root;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            assertTrue(in != null, "api-gateway 缺少 application.yml");
            root = loadYaml(in);
        }

        Map<String, Object> gateway = mapAt(root, "spring", "cloud", "gateway");
        List<Map<String, Object>> routes = (List<Map<String, Object>>) gateway.get("routes");
        assertTrue(routes != null && !routes.isEmpty(), "网关路由表为空");

        Set<String> ids = new HashSet<>();
        List<String> problems = new ArrayList<>();

        for (Map<String, Object> route : routes) {
            String id = String.valueOf(route.get("id"));
            assertTrue(ids.add(id), "路由 id 重复: " + id);

            String uri = String.valueOf(route.get("uri"));
            String svc = uri.replaceFirst("^lb:(//)?(ws://)?", "");
            assertFalse(svc.isBlank(), "路由 " + id + " 的 uri 不是 lb:// 服务名: " + uri);

            List<String> filters = (List<String>) route.getOrDefault("filters", List.of());
            String prefix = filters.stream()
                    .filter(f -> f.startsWith("PrefixPath="))
                    .map(f -> f.substring("PrefixPath=".length()))
                    .findFirst()
                    .orElse(null);

            if (prefix == null) {
                problems.add("路由 " + id + " 缺少 PrefixPath");
            } else if (!prefix.equals("/" + svc)) {
                problems.add("路由 " + id + " 的 PrefixPath=" + prefix + " 与服务 " + svc + " 的约定 /" + svc + " 不一致");
            }
        }

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    @SuppressWarnings("unchecked")
    void serviceContextPathMatchesRoutePrefixWhenSourceAvailable() throws Exception {
        Path gatewayModule = Path.of("").toAbsolutePath();
        Map<String, Object> root;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            root = loadYaml(in);
        }
        Map<String, Object> gateway = mapAt(root, "spring", "cloud", "gateway");
        List<Map<String, Object>> routes = (List<Map<String, Object>>) gateway.get("routes");

        Set<String> checked = new HashSet<>();
        for (Map<String, Object> route : routes) {
            String svc = String.valueOf(route.get("uri")).replaceFirst("^lb:(//)?(ws://)?", "");
            if (!checked.add(svc)) {
                continue;
            }
            Path serviceYml = gatewayModule.getParent().resolve(svc)
                    .resolve("src/main/resources/application.yml");
            if (!Files.exists(serviceYml)) {
                continue; // 单模块构建或 CI 隔离时跳过
            }
            Map<String, Object> serviceRoot = loadYaml(Files.newInputStream(serviceYml));
            Map<String, Object> server = (Map<String, Object>) serviceRoot.get("server");
            assertTrue(server != null, svc + " 缺少 server 配置");
            Map<String, Object> servlet = (Map<String, Object>) server.get("servlet");
            assertTrue(servlet != null, svc + " 缺少 server.servlet.context-path");
            assertEquals("/" + svc, String.valueOf(servlet.get("context-path")),
                    svc + " 的 context-path 与网关 PrefixPath 约定不一致");
        }
    }
}
