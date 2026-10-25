package com.typinggame.chat.service.impl;

import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.chat.repository.SystemNotificationRepository;
import com.typinggame.chat.repository.UserConversationRepository;
import com.typinggame.chat.repository.UserInfoRepository;
import com.typinggame.chat.service.InternalAdminService;
import com.typinggame.common.admin.AdminFilter;
import com.typinggame.common.admin.AdminPageSupport;
import com.typinggame.common.exception.BusinessException;
import com.typinggame.entity.message.FriendChatMessage;
import com.typinggame.entity.message.SystemNotification;
import com.typinggame.entity.message.UserConversation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class InternalAdminServiceImpl implements InternalAdminService {
    /** system_notification.notification_type：2-系统公告 */
    static final int TYPE_ANNOUNCEMENT = 2;

    private final UserConversationRepository userConversationRepository;
    private final FriendChatMessageRepository friendChatMessageRepository;
    private final SystemNotificationRepository systemNotificationRepository;
    private final UserInfoRepository userInfoRepository;

    public InternalAdminServiceImpl(UserConversationRepository userConversationRepository,
                                    FriendChatMessageRepository friendChatMessageRepository,
                                    SystemNotificationRepository systemNotificationRepository,
                                    UserInfoRepository userInfoRepository) {
        this.userConversationRepository = userConversationRepository;
        this.friendChatMessageRepository = friendChatMessageRepository;
        this.systemNotificationRepository = systemNotificationRepository;
        this.userInfoRepository = userInfoRepository;
    }

    @Override
    public Map<String, Object> getUserConversations(int page, int pageSize, Integer userId) {
        AdminFilter<UserConversation> filter = AdminFilter.of(UserConversation.class).eq("userId", userId);
        return AdminPageSupport.pageMap("items", userConversationRepository.findAll(filter, pageable(page, pageSize, "id")));
    }

    @Override
    public Map<String, Object> getFriendChatMessages(int page, int pageSize, Integer conversationId) {
        AdminFilter<FriendChatMessage> filter = AdminFilter.of(FriendChatMessage.class).eq("conversationId", conversationId);
        return AdminPageSupport.pageMap("items", friendChatMessageRepository.findAll(filter, pageable(page, pageSize, "id")));
    }

    @Override
    public Map<String, Object> getSystemNotifications(int page, int pageSize, Integer userId, Integer notificationType, Boolean isRead) {
        AdminFilter<SystemNotification> filter = AdminFilter.of(SystemNotification.class)
                .eq("userId", userId).eq("notificationType", notificationType).eq("isRead", isRead);
        return AdminPageSupport.pageMap("items", systemNotificationRepository.findAll(filter, pageable(page, pageSize, "id")));
    }

    @Override
    @Transactional
    public int publishNotification(Integer userId, String title, String content, Integer notificationType) {
        if (title == null || title.isBlank() || title.length() > 100) {
            throw BusinessException.badRequest("标题必须为 1-100 个字符");
        }
        if (content == null || content.isBlank() || content.length() > 500) {
            throw BusinessException.badRequest("内容必须为 1-500 个字符");
        }
        List<Integer> receivers;
        if (userId == null) {
            receivers = userInfoRepository.findAllUserIds();
        } else {
            if (!userInfoRepository.existsById(userId)) throw BusinessException.notFound("用户不存在: " + userId);
            receivers = List.of(userId);
        }
        int type = notificationType == null ? TYPE_ANNOUNCEMENT : notificationType;
        LocalDateTime now = LocalDateTime.now();
        List<SystemNotification> rows = receivers.stream().map(uid -> {
            SystemNotification n = new SystemNotification();
            n.setUserId(uid);
            n.setNotificationType(type);
            n.setNotificationTitle(title.trim());
            n.setNotificationContent(content.trim());
            n.setIsRead(false);
            n.setIsPersistent(true);
            n.setSendTime(now);
            n.setCreateTime(now);
            return n;
        }).toList();
        systemNotificationRepository.saveAll(rows);
        return rows.size();
    }

    @Override
    @Transactional
    public void deleteNotification(Integer id) {
        if (!systemNotificationRepository.existsById(id)) throw BusinessException.notFound("通知不存在");
        systemNotificationRepository.deleteById(id);
    }

    @Override
    public Map<String, Object> getOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return AdminPageSupport.map(
                "messageCount", friendChatMessageRepository.count(),
                "messagesToday", friendChatMessageRepository.count(AdminFilter.of(FriendChatMessage.class).between("sendTime", todayStart, null)),
                "notificationCount", systemNotificationRepository.count());
    }

    private PageRequest pageable(int page, int pageSize, String sortDescField) {
        int p = Math.max(0, page - 1);
        int size = Math.min(200, Math.max(1, pageSize));
        return PageRequest.of(p, size, Sort.by(Sort.Direction.DESC, sortDescField));
    }
}
