package com.typinggame.chat.controller;

import com.typinggame.chat.im.WsTicketService;
import com.typinggame.chat.service.MessageService;
import com.typinggame.dto.message.MessageDTO;
import com.typinggame.vo.ResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private WsTicketService wsTicketService;

    @GetMapping("/conversations")
    public ResponseEntity<ResultMessage<List<MessageDTO>>> getConversations() {
        Integer userId = getCurrentUserId();
        List<MessageDTO> conversations = messageService.getConversations(userId);
        return ResponseEntity.ok(ResultMessage.success(conversations));
    }

    @GetMapping
    public ResponseEntity<ResultMessage<List<MessageDTO>>> getMessages(
            @RequestParam(name = "friendId") Integer friendId,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "beforeId", required = false) Integer beforeId,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        Integer userId = getCurrentUserId();
        List<MessageDTO> messages = beforeId != null || offset == null
                ? messageService.getMessagesBefore(userId, friendId, beforeId, limit)
                : messageService.getMessages(userId, friendId, offset);
        return ResponseEntity.ok(ResultMessage.success(messages));
    }

    @PostMapping("/ws-ticket")
    public ResponseEntity<ResultMessage<Map<String, String>>> wsTicket(jakarta.servlet.http.HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("缺少访问令牌");
        }
        String ticket = wsTicketService.issue(header.substring(7), getCurrentUserId());
        return ResponseEntity.ok(ResultMessage.success(Map.of("ticket", ticket)));
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ResultMessage<List<MessageDTO>>> getConversationMessages(
            @PathVariable Integer conversationId,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "20") Integer size) {
        Integer userId = getCurrentUserId();
        List<MessageDTO> messages = messageService.getConversationMessages(userId, conversationId, page, size);
        return ResponseEntity.ok(ResultMessage.success(messages));
    }

    @PostMapping
    public ResponseEntity<ResultMessage<MessageDTO>> sendMessage(@RequestBody Map<String, Object> body) {
        Integer senderId = getCurrentUserId();
        Integer receiverId = body.get("receiverId") instanceof Number number ? number.intValue() : null;
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        String type = body.get("type") == null ? "text" : String.valueOf(body.get("type"));
        String clientMsgId = body.get("clientMsgId") == null ? null : String.valueOf(body.get("clientMsgId"));
        MessageDTO message = messageService.sendMessage(senderId, receiverId, content, type, clientMsgId);
        return ResponseEntity.ok(ResultMessage.success(message));
    }

    @PostMapping("/read")
    public ResponseEntity<ResultMessage<Boolean>> markMessagesAsRead() {
        Integer userId = getCurrentUserId();
        messageService.markAllAsRead(userId);
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @PostMapping("/read-by-friend")
    public ResponseEntity<ResultMessage<Boolean>> markAsReadByFriend(@RequestParam(name = "friendId") Integer friendId) {
        Integer userId = getCurrentUserId();
        messageService.markAsReadByFriend(userId, friendId);
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @PatchMapping("/{messageId}/read")
    public ResponseEntity<ResultMessage<Boolean>> markMessageAsRead(@PathVariable Integer messageId) {
        boolean result = messageService.markMessageAsRead(getCurrentUserId(), messageId);
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ResultMessage<Boolean>> deleteMessage(@PathVariable Integer messageId) {
        boolean result = messageService.deleteMessage(getCurrentUserId(), messageId);
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ResultMessage<Integer>> getUnreadCount() {
        Integer userId = getCurrentUserId();
        Integer unreadCount = messageService.getUnreadCount(userId);
        return ResponseEntity.ok(ResultMessage.success(unreadCount));
    }

    @GetMapping("/system")
    public ResponseEntity<ResultMessage<List<MessageDTO>>> getSystemMessages() {
        Integer userId = getCurrentUserId();
        List<MessageDTO> messages = messageService.getSystemMessages(userId);
        return ResponseEntity.ok(ResultMessage.success(messages));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}
