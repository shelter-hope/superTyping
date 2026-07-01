package com.typinggame.controller;

import com.typinggame.dto.user.UserStatsDTO;
import com.typinggame.dto.user.UserUpdateDTO;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.service.UserService;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 用户资料 API
 */
@RestController
@Tag(name = "用户资料 API")
@RequestMapping("/api/v1/profiles")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    @Operation(summary = "获取用户信息")
    public ResponseEntity<ResultMessage<UserInfo>> getUserById(@PathVariable("userId") Integer userId) {
        UserInfo userInfo = userService.getUserById(userId);
        return ResponseEntity.ok(ResultMessage.success(userInfo));
    }

    /**
     * 更新当前登录用户的信息（无需路径中的 userId，从 JWT 取当前用户，避免路径变量绑定问题）
     */
    @PatchMapping("/me")
    @Operation(summary = "更新当前用户信息")
    public ResponseEntity<ResultMessage<UserInfo>> updateCurrentUser(@Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        Integer userId = getCurrentUserId();
        UserInfo updatedUser = userService.updateUser(userId, userUpdateDTO);
        return ResponseEntity.ok(ResultMessage.success(updatedUser));
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "更新指定用户信息（路径形式，供管理或兼容）")
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

    /**
     * 搜索用户
     * @param username 用户名
     * @return 用户列表
     */
    @GetMapping
    @Operation(summary = "搜索用户")
    public ResponseEntity<ResultMessage<List<UserInfo>>> searchUsers(@RequestParam(required = false) String username) {
        // TODO: Implement search in service
        // List<UserInfo> users = userService.searchUsers(username);
        return ResponseEntity.ok(ResultMessage.success(List.of()));
    }

    /**
     * 上传用户头像
     * @param userId 用户ID
     * @param file 头像文件
     * @return 头像URL
     */
    @PostMapping("/{userId}/avatar")
    @Operation(summary = "上传用户头像")
    public ResponseEntity<ResultMessage<String>> uploadAvatar(@PathVariable("userId") Integer userId, @RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(userId, file);
        return ResponseEntity.ok(ResultMessage.success(avatarUrl));
    }

    /**
     * 获取用户游戏统计
     * @param userId 用户ID
     * @return 用户统计信息
     */
    @GetMapping("/{userId}/stats")
    @Operation(summary = "获取用户游戏统计")
    public ResponseEntity<ResultMessage<UserStatsDTO>> getUserStats(@PathVariable("userId") Integer userId) {
        UserStatsDTO stats = userService.getUserGameStats(userId);
        return ResponseEntity.ok(ResultMessage.success(stats));
    }
}
