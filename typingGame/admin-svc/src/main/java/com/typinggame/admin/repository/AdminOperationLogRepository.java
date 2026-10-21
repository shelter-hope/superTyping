package com.typinggame.admin.repository;

import com.typinggame.admin.entity.AdminOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long>, JpaSpecificationExecutor<AdminOperationLog> {
}
