package com.typinggame.chat.repository;

import com.typinggame.chat.entity.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Integer>, JpaSpecificationExecutor<SystemNotification> {
    List<SystemNotification> findByUserIdOrderBySendTimeDesc(Integer userId);

    Optional<SystemNotification> findByIdAndUserId(Integer id, Integer userId);

    long countByUserIdAndIsRead(Integer userId, Boolean isRead);
}
