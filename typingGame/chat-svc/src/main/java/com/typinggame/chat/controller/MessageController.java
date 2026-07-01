package com.typinggame.chat.controller;

import com.typinggame.chat.service.MessageService;
import com.typinggame.dto.message.MessageDTO;
import com.typinggame.vo.ResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/conversations")
    public ResponseEntity<ResultMessage<List<MessageDTO>>> getConversations() {
        Integer userId = getCurrentUserId();
        List<MessageDTO> conversations = messageService.getConversations(userId);
        return ResponseEntity.ok(ResultMessage.success(conversations));
    }

    @GetMapping
    public ResponseEntity<ResultMessage<List<MessageDTO>>> getMessages(
            @RequestParam(name = "friendId") Integer friendId,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset) {
        Integer userId = getCurrentUserId();
        List<MessageDTO> messages = messageService.getMessages(userId, friendId, offset);
        return ResponseEntity.ok(ResultMessage.success(messages));
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ResultMessage<List<MessageDTO>>> getConversationMessages(
            @PathVariable Integer conversationId,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "20") Integer size) {
        List<MessageDTO> messages = messageService.getConversationMessages(conversationId, page, size);
        return ResponseEntity.ok(ResultMessage.success(messages));
    }

    @PostMapping
    public ResponseEntity<ResultMessage<MessageDTO>> sendMessage(
            @RequestParam(name = "receiverId") Integer receiverId,
            @RequestParam(name = "content") String content,
            @RequestParam(name = "type", defaultValue = "text") String type) {
        Integer senderId = getCurrentUserId();
        MessageDTO message = messageService.sendMessage(senderId, receiverId, content, type);
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
        boolean result = messageService.markMessageAsRead(messageId);
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ResultMessage<Boolean>> deleteMessage(@PathVariable Integer messageId) {
        boolean result = messageService.deleteMessage(messageId);
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