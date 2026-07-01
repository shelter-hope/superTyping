package com.typinggame.user.service;

import com.typinggame.dto.auth.ChangePasswordDTO;
import com.typinggame.dto.auth.TokenResponseDTO;
import com.typinggame.dto.auth.UserLoginDTO;
import com.typinggame.dto.auth.UserRegisterDTO;
import com.typinggame.dto.user.UserDTO;

public interface AuthService {
    TokenResponseDTO register(UserRegisterDTO userRegisterDTO);

    TokenResponseDTO login(UserLoginDTO userLoginDTO);

    boolean validateToken(String token);

    TokenResponseDTO refreshToken(String refreshToken);

    UserDTO me(Integer userId);

    boolean changePassword(Integer userId, ChangePasswordDTO changePasswordDTO);
}