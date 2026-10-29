package com.typinggame.dto.friend;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友请求DTO
 */
@Data
public class FriendRequestDTO {
    private Integer id;
    private Integer senderId;
    private String senderName;
    private Integer receiverId;
    private String receiverName;
    private LocalDateTime sendTime;
    private String status;
    private String applyMessage;
}