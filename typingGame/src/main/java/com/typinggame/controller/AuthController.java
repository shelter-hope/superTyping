package com.typinggame.controller;

import com.typinggame.dto.auth.ChangePasswordDTO;
import com.typinggame.dto.auth.TokenResponseDTO;
import com.typinggame.dto.auth.UserLoginDTO;
import com.typinggame.dto.auth.UserRegisterDTO;
import com.typinggame.dto.user.UserDTO;
import com.typinggame.service.AuthService;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 认证相关 API
 */
@RestController
@Tag(name = "认证相关 API")
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger log = LogManager.getLogger(AuthController.class);
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册
     * @param userRegisterDTO 用户注册请求
     * @return 注册结果
     */
    @PostMapping("/signup")
    @Operation(summary = "用户注册")
    public ResponseEntity<ResultMessage<TokenResponseDTO>> register(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        TokenResponseDTO tokenResponseDTO = authService.register(userRegisterDTO);
        return ResponseEntity.ok(ResultMessage.success(tokenResponseDTO));
    }

    /**
     * 用户登录
     * @param userLoginDTO 用户登录请求
     * @return 令牌响应
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseEntity<ResultMessage<TokenResponseDTO>> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        TokenResponseDTO tokenResponseDTO = authService.login(userLoginDTO);
        return ResponseEntity.ok(ResultMessage.success(tokenResponseDTO));
    }

    /**
     * 刷新令牌
     * @param refreshToken 刷新令牌
     * @return 新的令牌响应
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌")
    public ResponseEntity<ResultMessage<TokenResponseDTO>> refreshToken(@RequestParam String refreshToken) {
        TokenResponseDTO tokenResponseDTO = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ResultMessage.success(tokenResponseDTO));
    }

    /**
     * 获取当前用户
     * @return 当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户")
    public ResponseEntity<ResultMessage<UserDTO>> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 假设 authentication.getName() 返回的是 userId
        Integer userId = Integer.parseInt(authentication.getName());
        UserDTO userDTO = authService.me(userId);
        return ResponseEntity.ok(ResultMessage.success(userDTO));
    }

    /**
     * 登出
     * @return 成功
     */
    @PostMapping("/logout")
    @Operation(summary = "登出")
    public ResponseEntity<ResultMessage<Void>> logout() {
        // JWT 无状态，客户端删除 token 即可
        return ResponseEntity.ok(ResultMessage.success());
    }

    /**
     * 修改密码
     * @param changePasswordDTO 修改密码请求
     * @return 修改结果
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密码")
    public ResponseEntity<ResultMessage<Boolean>> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        // 从SecurityContext中获取当前用户ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = Integer.parseInt(authentication.getName());

        boolean result = authService.changePassword(userId, changePasswordDTO);
        return ResponseEntity.ok(ResultMessage.success(result));
    }
}
