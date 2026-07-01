package com.typinggame.user.controller;

import com.typinggame.dto.friend.FriendRequestDTO;
import com.typinggame.dto.friend.FriendResponseDTO;
import com.typinggame.user.service.FriendService;
import com.typinggame.vo.ResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @GetMapping
    public ResponseEntity<ResultMessage<List<FriendResponseDTO>>> getFriendList() {
        Integer userId = getCurrentUserId();
        List<FriendResponseDTO> friendList = friendService.getFriendList(userId);
        return ResponseEntity.ok(ResultMessage.success(friendList));
    }

    @GetMapping("/search")
    public ResponseEntity<ResultMessage<List<FriendResponseDTO>>> searchUsers(@RequestParam("keyword") String keyword) {
        Integer userId = getCurrentUserId();
        List<FriendResponseDTO> searchResults = friendService.searchUsers(userId, keyword);
        return ResponseEntity.ok(ResultMessage.success(searchResults));
    }

    @PostMapping("/request")
    public ResponseEntity<ResultMessage<FriendRequestDTO>> sendFriendRequest(@RequestParam("receiverId") Integer receiverId) {
        Integer senderId = getCurrentUserId();
        FriendRequestDTO friendRequest = friendService.sendFriendRequest(senderId, receiverId);
        return ResponseEntity.ok(ResultMessage.success(friendRequest));
    }

    @PatchMapping("/request/{id}")
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

    @GetMapping("/request")
    public ResponseEntity<ResultMessage<List<FriendRequestDTO>>> getFriendRequests() {
        Integer userId = getCurrentUserId();
        List<FriendRequestDTO> friendRequests = friendService.getFriendRequests(userId);
        return ResponseEntity.ok(ResultMessage.success(friendRequests));
    }

    @DeleteMapping("/{friendId}")
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