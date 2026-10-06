package com.typinggame.user.controller;

import com.typinggame.dto.auth.ChangePasswordDTO;
import com.typinggame.dto.auth.TokenResponseDTO;
import com.typinggame.dto.auth.UserLoginDTO;
import com.typinggame.dto.auth.UserRegisterDTO;
import com.typinggame.dto.user.UserDTO;
import com.typinggame.user.service.AuthService;
import com.typinggame.vo.ResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ResultMessage<TokenResponseDTO>> register(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        TokenResponseDTO tokenResponseDTO = authService.register(userRegisterDTO);
        return ResponseEntity.ok(ResultMessage.success(tokenResponseDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<ResultMessage<TokenResponseDTO>> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        TokenResponseDTO tokenResponseDTO = authService.login(userLoginDTO);
        return ResponseEntity.ok(ResultMessage.success(tokenResponseDTO));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResultMessage<TokenResponseDTO>> refreshToken(@RequestParam String refreshToken) {
        TokenResponseDTO tokenResponseDTO = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ResultMessage.success(tokenResponseDTO));
    }

    @GetMapping("/me")
    public ResponseEntity<ResultMessage<UserDTO>> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = Integer.parseInt(authentication.getName());
        UserDTO userDTO = authService.me(userId);
        return ResponseEntity.ok(ResultMessage.success(userDTO));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResultMessage<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "refreshToken", required = false) String refreshToken) {
        String accessToken = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : null;
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(ResultMessage.success());
    }

    @PostMapping("/change-password")
    public ResponseEntity<ResultMessage<Boolean>> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = Integer.parseInt(authentication.getName());

        boolean result = authService.changePassword(userId, changePasswordDTO);
        return ResponseEntity.ok(ResultMessage.success(result));
    }
}
