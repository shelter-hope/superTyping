package com.typinggame.dto.friend;

import lombok.Data;

/**
 * 好友响应DTO
 */
@Data
public class FriendResponseDTO {
    private Integer id;
    private Integer userId;
    private String username;
    private String avatar;
    private String bio; // 个性签名
    private Boolean isOnline;
    private String lastActiveTime;
}