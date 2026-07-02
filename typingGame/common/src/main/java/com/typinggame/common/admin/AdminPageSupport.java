package com.typinggame.common.admin;

import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.Map;

public class AdminPageSupport {

    private AdminPageSupport() {
    }

    public static Map<String, Object> map(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    public static Map<String, Object> pageMap(String itemsKey, Page<?> page) {
        return map(itemsKey, page.getContent(), "total", page.getTotalElements());
    }
}
