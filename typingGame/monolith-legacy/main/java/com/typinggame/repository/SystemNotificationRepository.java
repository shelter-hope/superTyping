package com.typinggame.repository;

import com.typinggame.entity.message.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统消息通知表数据访问层
 */
@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Integer> {
}
