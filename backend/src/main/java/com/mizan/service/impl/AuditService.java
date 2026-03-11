package com.mizan.service.impl;

import com.mizan.entity.AuditLog;
import com.mizan.enums.AuditAction;
import com.mizan.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Audit logging service.
 * - If RabbitMQ is available: publishes events to queue (async, non-blocking).
 * - If RabbitMQ is disabled (free tier): writes directly to DB via @Async.
 * - If both fail: logs to file and moves on (never blocks auth flow).
 */
@Slf4j
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepo;
    private final RabbitTemplate rabbitTemplate; // null when RabbitMQ disabled

    public AuditService(AuditLogRepository auditLogRepo,
                        @Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.auditLogRepo = auditLogRepo;
        this.rabbitTemplate = rabbitTemplate;

        if (rabbitTemplate != null) {
            log.info("AuditService: RabbitMQ available — using async queue");
        } else {
            log.info("AuditService: RabbitMQ not available — using direct DB writes");
        }
    }

    /**
     * Log an audit event. Never throws, never blocks.
     */
    @Async
    public void logAsync(UUID userId, AuditAction action, String ipAddress,
                         String deviceInfo, Map<String, Object> details) {
        AuditLog entry = AuditLog.builder()
                .userId(userId)
                .action(action)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .details(details)
                .createdAt(Instant.now())
                .build();

        // Try RabbitMQ first
        if (rabbitTemplate != null) {
            try {
                rabbitTemplate.convertAndSend("mizan.audit", "audit.log", entry);
                log.info("audit: action={} userId={} ip={}", action, userId, ipAddress);
                return;
            } catch (Exception e) {
                log.warn("RabbitMQ publish failed, falling back to DB: {}", e.getMessage());
            }
        }

        // Fallback: direct DB write
        try {
            auditLogRepo.save(entry);
            log.info("audit(db): action={} userId={} ip={}", action, userId, ipAddress);
        } catch (Exception e) {
            // Last resort: just log it
            log.error("Audit log failed completely: action={} userId={} error={}",
                    action, userId, e.getMessage());
        }
    }
}
