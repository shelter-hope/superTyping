package com.typinggame.controller;

import com.typinggame.dto.friend.FriendRequestDTO;
import com.typinggame.dto.friend.FriendResponseDTO;
import com.typinggame.service.FriendService;
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
 * 好友管理 API
 */
@RestController
@Tag(name = "好友管理 API")
@RequestMapping("/api/v1/friends")
public class FriendController {

    @Autowired
    private FriendService friendService;

    /**
     * 获取好友列表
     * @return 好友列表
     */
    @GetMapping
    @Operation(summary = "获取好友列表")
    public ResponseEntity<ResultMessage<List<FriendResponseDTO>>> getFriendList() {
        Integer userId = getCurrentUserId();
        List<FriendResponseDTO> friendList = friendService.getFriendList(userId);
        return ResponseEntity.ok(ResultMessage.success(friendList));
    }

    /**
     * 搜索用户
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索用户")
    public ResponseEntity<ResultMessage<List<FriendResponseDTO>>> searchUsers(@RequestParam("keyword") String keyword) {
        Integer userId = getCurrentUserId();
        List<FriendResponseDTO> searchResults = friendService.searchUsers(userId, keyword);
        return ResponseEntity.ok(ResultMessage.success(searchResults));
    }

    /**
     * 发送好友请求
     * @param receiverId 接收者ID
     * @return 好友请求DTO
     */
    @PostMapping("/request")
    @Operation(summary = "发送好友请求")
    public ResponseEntity<ResultMessage<FriendRequestDTO>> sendFriendRequest(@RequestParam("receiverId") Integer receiverId) {
        Integer senderId = getCurrentUserId();
        FriendRequestDTO friendRequest = friendService.sendFriendRequest(senderId, receiverId);
        return ResponseEntity.ok(ResultMessage.success(friendRequest));
    }

    /**
     * 处理好友请求 (接受/拒绝)
     * @param id 请求ID
     * @param status 状态 (accepted/rejected)
     * @return 操作结果
     */
    @PatchMapping("/request/{id}")
    @Operation(summary = "处理好友请求")
    public ResponseEntity<ResultMessage<Boolean>> handleFriendRequest(
            @PathVariable("id") Integer id,
            @RequestParam("status") String status) {
        boolean result;
        if ("accepted".equalsIgnoreCase(status)) {
            result = friendService.acceptFriendRequest(id);
        } else if ("rejected".equalsIgnoreCase(status)) {
            result = friendService.rejectFriendRequest(id);
        } else {
            throw new IllegalArgumentException("Invalid status");
        }
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    /**
     * 获取好友请求列表
     * @return 好友请求列表
     */
    @GetMapping("/request")
    @Operation(summary = "获取好友请求列表")
    public ResponseEntity<ResultMessage<List<FriendRequestDTO>>> getFriendRequests() {
        Integer userId = getCurrentUserId();
        List<FriendRequestDTO> friendRequests = friendService.getFriendRequests(userId);
        return ResponseEntity.ok(ResultMessage.success(friendRequests));
    }

    /**
     * 删除好友
     * @param friendId 好友ID
     * @return 操作结果
     */
    @DeleteMapping("/{friendId}")
    @Operation(summary = "删除好友")
    public ResponseEntity<ResultMessage<Boolean>> deleteFriend(@PathVariable("friendId") Integer friendId) {
        Integer userId = getCurrentUserId();
        boolean result = friendService.deleteFriend(userId, friendId);
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}
