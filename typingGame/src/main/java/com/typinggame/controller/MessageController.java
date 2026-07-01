package com.typinggame.controller;

import com.typinggame.dto.message.MessageDTO;
import com.typinggame.service.MessageService;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息管理 API
 */
@RestController
@Tag(name = "消息管理 API")
@RequestMapping("/api/v1/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 获取会话列表
     * @return 会话列表
     */
    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表")
    public ResponseEntity<ResultMessage<List<MessageDTO>>> getConversations() {
        Integer userId = getCurrentUserId();
        List<MessageDTO> conversations = messageService.getConversations(userId);
        return ResponseEntity.ok(ResultMessage.success(conversations));
    }

    /**
     * 获取与特定好友的消息历史
     * @param friendId 好友ID
     * @param offset 偏移量
     * @return 消息列表
     */
    @GetMapping
    @Operation(summary = "获取消息历史")
    public ResponseEntity<ResultMessage<List<MessageDTO>>> getMessages(
            @RequestParam(name = "friendId") Integer friendId,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset) {
        Integer userId = getCurrentUserId();
        List<MessageDTO> messages = messageService.getMessages(userId, friendId, offset);
        return ResponseEntity.ok(ResultMessage.success(messages));
    }

    /**
     * 获取会话消息 (旧接口适配)
     * @param conversationId 会话ID
     * @param page 页码
     * @param size 每页大小
     * @return 会话消息列表
     */
    @GetMapping("/conversation/{conversationId}")
    @Operation(summary = "获取会话消息")
    public ResponseEntity<ResultMessage<List<MessageDTO>>> getConversationMessages(
            @PathVariable Integer conversationId,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "20") Integer size) {
        List<MessageDTO> messages = messageService.getConversationMessages(conversationId, page, size);
        return ResponseEntity.ok(ResultMessage.success(messages));
    }

    /**
     * 发送消息
     * @param receiverId 接收者ID
     * @param content 消息内容
     * @param type 消息类型
     * @return 发送的消息
     */
    @PostMapping
    @Operation(summary = "发送消息")
    public ResponseEntity<ResultMessage<MessageDTO>> sendMessage(
            @RequestParam(name = "receiverId") Integer receiverId,
            @RequestParam(name = "content") String content,
            @RequestParam(name = "type", defaultValue = "text") String type) {
        Integer senderId = getCurrentUserId();
        MessageDTO message = messageService.sendMessage(senderId, receiverId, content, type);
        return ResponseEntity.ok(ResultMessage.success(message));
    }

    /**
     * 批量标记消息已读
     * @return 操作结果
     */
    @PostMapping("/read")
    @Operation(summary = "批量标记已读")
    public ResponseEntity<ResultMessage<Boolean>> markMessagesAsRead() {
        Integer userId = getCurrentUserId();
        messageService.markAllAsRead(userId);
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    /**
     * 用户进入与某好友的聊天界面时调用：将该会话内当前用户收到的未读消息阅读状态置为 1（已读）
     * @param friendId 好友用户ID
     * @return 操作结果
     */
    @PostMapping("/read-by-friend")
    @Operation(summary = "按好友标记会话已读（read_status=1）")
    public ResponseEntity<ResultMessage<Boolean>> markAsReadByFriend(@RequestParam(name = "friendId") Integer friendId) {
        Integer userId = getCurrentUserId();
        messageService.markAsReadByFriend(userId, friendId);
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    /**
     * 标记单条消息已读
     * @param messageId 消息ID
     * @return 操作结果
     */
    @PatchMapping("/{messageId}/read")
    @Operation(summary = "标记消息已读")
    public ResponseEntity<ResultMessage<Boolean>> markMessageAsRead(@PathVariable Integer messageId) {
        boolean result = messageService.markMessageAsRead(messageId);
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    /**
     * 删除消息
     * @param messageId 消息ID
     * @return 操作结果
     */
    @DeleteMapping("/{messageId}")
    @Operation(summary = "删除消息")
    public ResponseEntity<ResultMessage<Boolean>> deleteMessage(@PathVariable Integer messageId) {
        boolean result = messageService.deleteMessage(messageId);
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    /**
     * 获取未读消息数
     * @return 未读消息数
     */
    @GetMapping("/unread-count")
    @Operation(summary = "获取未读消息数")
    public ResponseEntity<ResultMessage<Integer>> getUnreadCount() {
        Integer userId = getCurrentUserId();
        Integer unreadCount = messageService.getUnreadCount(userId);
        return ResponseEntity.ok(ResultMessage.success(unreadCount));
    }

    /**
     * 获取系统消息
     * @return 系统消息列表
     */
    @GetMapping("/system")
    @Operation(summary = "获取系统消息")
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
