package com.typinggame.dto.user;

import lombok.Data;

/**
 * 用户更新请求DTO
 */
@Data
public class UserUpdateDTO {
    private String username;
    private String nickname; // 新增昵称字段
    private String avatar;
    private String signature; // 对应 signature
}
