package com.typinggame.chat.im;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.dto.message.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 下行事件：
 * <pre>
 * {"type":"MESSAGE","message":{...MessageDTO}}
 * {"type":"READ","readerId":5}     // readerId 读完了与接收方的会话
 * </pre>
 * 推送失败只记日志，不影响 REST 主流程。
 */
@Slf4j
@Component
public class ImNotifier {

    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_READ = "READ";

    private final ImSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public ImNotifier(ImSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    public void pushMessage(Integer toUserId, MessageDTO message) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", TYPE_MESSAGE);
        event.put("message", message);
        push(toUserId, event);
    }

    public void pushRead(Integer toUserId, Integer readerId) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", TYPE_READ);
        event.put("readerId", readerId);
        push(toUserId, event);
    }

    private void push(Integer toUserId, Map<String, Object> event) {
        if (toUserId == null || !sessionRegistry.isOnline(toUserId)) {
            return;
        }
        try {
            sessionRegistry.sendToUser(toUserId, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("Serialize IM event failed, type={}", event.get("type"), e);
        } catch (RuntimeException e) {
            log.warn("IM push failed, toUserId={}", toUserId, e);
        }
    }
}
