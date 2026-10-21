package com.typinggame.admin.service;

import com.typinggame.admin.entity.AdminOperationLog;
import com.typinggame.admin.repository.AdminOperationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogServiceTest {

    private AdminOperationLogRepository repository;
    private OperationLogService service;

    @BeforeEach
    void setUp() {
        repository = mock(AdminOperationLogRepository.class);
        service = new OperationLogService(repository);
    }

    private AdminOperationLog saved() {
        ArgumentCaptor<AdminOperationLog> captor = ArgumentCaptor.forClass(AdminOperationLog.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void truncatesLongBodyAndMarksSuccess() {
        String body = "x".repeat(OperationLogService.MAX_BODY + 500);

        service.record(1, "PATCH", "/api/v1/admin/users/2/status", null, body, 200, "127.0.0.1", 12);

        AdminOperationLog log = saved();
        assertEquals(OperationLogService.MAX_BODY, log.getRequestBody().length());
        assertTrue(log.getSuccess());
        assertEquals(1, log.getOperatorId());
    }

    @Test
    void failedResponseIsRecordedAsFailure() {
        service.record(1, "DELETE", "/api/v1/admin/config-templates/9", null, null, 404, null, 3);

        assertFalse(saved().getSuccess());
    }

    @Test
    void repositoryErrorDoesNotPropagate() {
        when(repository.save(any())).thenThrow(new IllegalStateException("db down"));

        assertDoesNotThrow(() -> service.record(1, "POST", "/x", null, "{}", 200, null, 1));
    }
}
