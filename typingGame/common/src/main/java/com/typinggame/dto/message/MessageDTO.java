package com.typinggame.dto.message;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息DTO
 */
@Data
public class MessageDTO {
    private Integer id;
    private Integer senderId;
    private String senderName;
    private String senderAvatar; // Add avatar
    private Integer receiverId; // 接受者ID
    private String receiverName; // 接受者名字
    private String content; // 内容
    private LocalDateTime sendTime; // 发送时间
    private Boolean isRead; // 是否已读
    private String type;  // 类型 好友之间/管理员/系统 各有自己的类型
    
    // For conversation list
    private Integer conversationId;
    private Integer unreadCount;
    private String clientMsgId;
    private Integer serverSeq;
}
