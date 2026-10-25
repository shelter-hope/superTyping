package com.typinggame.chat.service;

import java.util.Map;

public interface InternalAdminService {
    Map<String, Object> getUserConversations(int page, int pageSize, Integer userId);
    Map<String, Object> getFriendChatMessages(int page, int pageSize, Integer conversationId);
    Map<String, Object> getSystemNotifications(int page, int pageSize, Integer userId, Integer notificationType, Boolean isRead);

    /**
     * 发布系统公告。userId 为空时发给全部用户（逐用户写入，保证每人独立的已读状态）。
     * @return 实际写入的通知条数
     */
    int publishNotification(Integer userId, String title, String content, Integer notificationType);

    void deleteNotification(Integer id);

    Map<String, Object> getOverview();
}
