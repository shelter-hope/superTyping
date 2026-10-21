package com.typinggame.chat.im;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AckTracker {
    private final long retryAfterMillis;
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();

    public AckTracker(long retryAfterMillis) {
        this.retryAfterMillis = retryAfterMillis;
    }

    public void expect(String clientMsgId, String payload, long now) {
        if (clientMsgId == null || clientMsgId.isBlank()) return;
        pending.put(clientMsgId, new Pending(payload, now));
    }

    public void ack(String clientMsgId) {
        if (clientMsgId != null) pending.remove(clientMsgId);
    }

    public List<String> due(long now) {
        List<String> payloads = new ArrayList<>();
        pending.forEach((id, item) -> {
            if (now - item.sentAt >= retryAfterMillis) {
                payloads.add(item.payload);
                pending.put(id, new Pending(item.payload, now));
            }
        });
        return payloads;
    }

    private record Pending(String payload, long sentAt) {
    }
}
