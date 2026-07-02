package com.typinggame.chat.repository;

import com.typinggame.entity.message.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Integer> {
}
