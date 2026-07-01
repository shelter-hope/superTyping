package com.typinggame.service.impl;

import com.typinggame.dto.auth.ChangePasswordDTO;
import com.typinggame.dto.auth.TokenResponseDTO;
import com.typinggame.dto.auth.UserLoginDTO;
import com.typinggame.dto.auth.UserRegisterDTO;
import com.typinggame.dto.user.UserDTO;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.repository.UserInfoRepository;
import com.typinggame.security.JwtTokenProvider;
import com.typinggame.service.AuthService;
import com.typinggame.service.UserRegistrationInitService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserInfoRepository userInfoRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRegistrationInitService userRegistrationInitService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpiration;

    @Override
    public TokenResponseDTO register(UserRegisterDTO userRegisterDTO) {
        // 检查用户名是否已存在
        if (userInfoRepository.findByUsername(userRegisterDTO.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(userRegisterDTO.getUsername());
        userInfo.setPassword(passwordEncoder.encode(userRegisterDTO.getPassword()));
        userInfo.setNickname(userRegisterDTO.getNickname());
        userInfo.setRole("user");

        // 保存用户
        UserInfo savedUser = userInfoRepository.save(userInfo);

        // 新用户基础表初始化（失败仅打日志，不影响注册成功）
        try {
            userRegistrationInitService.initForNewUser(savedUser);
        } catch (Exception e) {
            log.error("Init base config for new user failed, userId={}", savedUser.getUserId(), e);
        }

        // 注册成功后自动登录
        return generateTokenResponse(savedUser);
    }

    @Override
    public TokenResponseDTO login(UserLoginDTO userLoginDTO) {

        String username =  userLoginDTO.getUsername();
        // 手动校验
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("用户名长度必须在3-50个字符之间");
        }

        // 查找用户
        UserInfo userInfo = userInfoRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // 验证密码
        if (!passwordEncoder.matches(userLoginDTO.getPassword(), userInfo.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        return generateTokenResponse(userInfo);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    @Override
    public TokenResponseDTO refreshToken(String refreshToken) {
        // 验证刷新令牌
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("无效的刷新令牌");
        }

        // 获取用户ID
        Integer userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 查找用户
        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return generateTokenResponse(userInfo);
    }

    @Override
    public UserDTO me(Integer userId) {
        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return convertToUserDTO(userInfo);
    }

    @Override
    public boolean changePassword(Integer userId, ChangePasswordDTO changePasswordDTO) {
        // 查找用户
        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证旧密码
        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), userInfo.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        // 验证新密码和确认密码是否一致
        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
            throw new RuntimeException("新密码和确认密码不一致");
        }

        // 更新密码
        userInfo.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        userInfoRepository.save(userInfo);

        return true;
    }

    private TokenResponseDTO generateTokenResponse(UserInfo userInfo) {
        // 生成令牌
        String accessToken = jwtTokenProvider.generateToken(userInfo.getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userInfo.getUserId());

        // 构建响应
        TokenResponseDTO tokenResponseDTO = new TokenResponseDTO();
        tokenResponseDTO.setAccessToken(accessToken);
        tokenResponseDTO.setRefreshToken(refreshToken);
        tokenResponseDTO.setExpiresIn(jwtExpiration);
        tokenResponseDTO.setRefreshExpiresIn(jwtRefreshExpiration);
        tokenResponseDTO.setTokenType("Bearer");
        tokenResponseDTO.setUser(convertToUserDTO(userInfo));

        return tokenResponseDTO;
    }

    private UserDTO convertToUserDTO(UserInfo userInfo) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userInfo.getUserId());
        userDTO.setUsername(userInfo.getUsername());
        userDTO.setNickname(userInfo.getNickname());
        userDTO.setAvatar(userInfo.getAvatar());
        userDTO.setRole(userInfo.getRole());
        return userDTO;
    }
}
