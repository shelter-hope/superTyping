package com.typinggame.dto.auth;

import com.typinggame.dto.user.UserDTO;
import lombok.Data;

/**
 * 令牌响应DTO
 */
@Data
public class TokenResponseDTO {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private long refreshExpiresIn;
    private String tokenType;
    private UserDTO user;
}
