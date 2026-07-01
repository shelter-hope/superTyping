package com.typinggame.controller.auth;

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

    @PostMapping("/signup")
    @Operation(summary = "用户注册")
    public ResponseEntity<ResultMessage<TokenResponseDTO>> register(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        TokenResponseDTO tokenResponseDTO = authService.register(userRegisterDTO);
        return ResponseEntity.ok(ResultMessage.success(tokenResponseDTO));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseEntity<ResultMessage<TokenResponseDTO>> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        TokenResponseDTO tokenResponseDTO = authService.login(userLoginDTO);
        return ResponseEntity.ok(ResultMessage.success(tokenResponseDTO));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌")
    public ResponseEntity<ResultMessage<TokenResponseDTO>> refreshToken(@RequestParam String refreshToken) {
        TokenResponseDTO tokenResponseDTO = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ResultMessage.success(tokenResponseDTO));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户")
    public ResponseEntity<ResultMessage<UserDTO>> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = Integer.parseInt(authentication.getName());
        UserDTO userDTO = authService.me(userId);
        return ResponseEntity.ok(ResultMessage.success(userDTO));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出")
    public ResponseEntity<ResultMessage<Void>> logout() {
        return ResponseEntity.ok(ResultMessage.success());
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码")
    public ResponseEntity<ResultMessage<Boolean>> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = Integer.parseInt(authentication.getName());

        boolean result = authService.changePassword(userId, changePasswordDTO);
        return ResponseEntity.ok(ResultMessage.success(result));
    }
}