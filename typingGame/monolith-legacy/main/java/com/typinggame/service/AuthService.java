package com.typinggame.service;

import com.typinggame.dto.auth.ChangePasswordDTO;
import com.typinggame.dto.auth.TokenResponseDTO;
import com.typinggame.dto.auth.UserLoginDTO;
import com.typinggame.dto.auth.UserRegisterDTO;
import com.typinggame.dto.user.UserDTO;

/**
 * 处理用户认证、授权相关逻辑
 */
public interface AuthService {
    /**
     * 用户注册
     * @param userRegisterDTO 用户注册请求
     * @return 令牌响应
     */
    TokenResponseDTO register(UserRegisterDTO userRegisterDTO);

    /**
     * 用户登录
     * @param userLoginDTO 用户登录请求
     * @return 令牌响应
     */
    TokenResponseDTO login(UserLoginDTO userLoginDTO);

    /**
     * 验证JWT令牌
     * @param token JWT令牌
     * @return 验证结果
     */
    boolean validateToken(String token);

    /**
     * 刷新令牌
     * @param refreshToken 刷新令牌
     * @return 新的令牌响应
     */
    TokenResponseDTO refreshToken(String refreshToken);

    /**
     * 获取当前用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    UserDTO me(Integer userId);

    /**
     * 修改密码
     * @param userId 用户ID
     * @param changePasswordDTO 修改密码请求
     * @return 修改结果
     */
    boolean changePassword(Integer userId, ChangePasswordDTO changePasswordDTO);
}
