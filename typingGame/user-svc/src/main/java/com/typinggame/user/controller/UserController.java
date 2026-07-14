package com.typinggame.user.controller;

import com.typinggame.dto.user.UserStatsDTO;
import com.typinggame.dto.user.UserUpdateDTO;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.user.service.UserService;
import com.typinggame.vo.ResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/profiles")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<ResultMessage<UserInfo>> getUserById(@PathVariable("userId") Integer userId) {
        UserInfo userInfo = userService.getUserById(userId);
        return ResponseEntity.ok(ResultMessage.success(userInfo));
    }

    @PatchMapping("/me")
    public ResponseEntity<ResultMessage<UserInfo>> updateCurrentUser(@Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        Integer userId = getCurrentUserId();
        UserInfo updatedUser = userService.updateUser(userId, userUpdateDTO);
        return ResponseEntity.ok(ResultMessage.success(updatedUser));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ResultMessage<UserInfo>> updateUser(@PathVariable("userId") Integer userId, @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        UserInfo updatedUser = userService.updateUser(userId, userUpdateDTO);
        return ResponseEntity.ok(ResultMessage.success(updatedUser));
    }

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !StringUtils.hasText(auth.getName())) {
            throw new RuntimeException("未登录或无法获取用户ID");
        }
        return Integer.parseInt(auth.getName());
    }

    @GetMapping
    public ResponseEntity<ResultMessage<List<UserInfo>>> searchUsers(@RequestParam(required = false) String username, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<UserInfo> userPage = userService.getUsers(PageRequest.of(page, size), username);
        return ResponseEntity.ok(ResultMessage.success(userPage.getContent()));
    }

    @PostMapping("/{userId}/avatar")
    public ResponseEntity<ResultMessage<String>> uploadAvatar(@PathVariable("userId") Integer userId, @RequestParam("file") MultipartFile file) {
        if (!getCurrentUserId().equals(userId)) {
            throw new AccessDeniedException("只能上传自己的头像");
        }
        String avatarUrl = userService.uploadAvatar(userId, file);
        return ResponseEntity.ok(ResultMessage.success(avatarUrl));
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<ResultMessage<UserStatsDTO>> getUserStats(@PathVariable("userId") Integer userId) {
        UserStatsDTO stats = userService.getUserGameStats(userId);
        return ResponseEntity.ok(ResultMessage.success(stats));
    }
}
