package com.typinggame.user.service.impl;

import com.typinggame.dto.auth.ChangePasswordDTO;
import com.typinggame.dto.auth.TokenResponseDTO;
import com.typinggame.dto.auth.UserLoginDTO;
import com.typinggame.dto.auth.UserRegisterDTO;
import com.typinggame.dto.user.UserDTO;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.user.repository.UserInfoRepository;
import com.typinggame.user.security.JwtTokenProvider;
import com.typinggame.user.service.AuthService;
import com.typinggame.user.service.UserRegistrationInitService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
        if (userInfoRepository.findByUsername(userRegisterDTO.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(userRegisterDTO.getUsername());
        userInfo.setPassword(passwordEncoder.encode(userRegisterDTO.getPassword()));
        userInfo.setNickname(userRegisterDTO.getNickname());
        userInfo.setRole("user");

        UserInfo savedUser = userInfoRepository.save(userInfo);

        try {
            userRegistrationInitService.initForNewUser(savedUser);
        } catch (Exception e) {
            log.error("Init base config for new user failed, userId={}", savedUser.getUserId(), e);
        }

        return generateTokenResponse(savedUser);
    }

    @Override
    public TokenResponseDTO login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("用户名长度必须在3-50个字符之间");
        }

        UserInfo userInfo = userInfoRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

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
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("无效的刷新令牌");
        }

        Integer userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

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
        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), userInfo.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
            throw new RuntimeException("新密码和确认密码不一致");
        }

        userInfo.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        userInfoRepository.save(userInfo);

        return true;
    }

    private TokenResponseDTO generateTokenResponse(UserInfo userInfo) {
        String accessToken = jwtTokenProvider.generateToken(userInfo.getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userInfo.getUserId());

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