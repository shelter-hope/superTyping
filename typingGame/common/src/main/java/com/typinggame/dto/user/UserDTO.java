package com.typinggame.dto.user;

import lombok.Data;

@Data
public class UserDTO {
    private Integer userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
}
