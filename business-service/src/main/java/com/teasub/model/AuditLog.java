package com.teasub.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    private String actorId;
    private String actorRole;
    private String action;
    private String targetId;
    private Map<String, Object> metadata;
    private Instant timestamp = Instant.now();

    public static AuditLog of(String actorId, String actorRole, String action, String targetId, Map<String, Object> metadata) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setActorRole(actorRole);
        log.setAction(action);
        log.setTargetId(targetId);
        log.setMetadata(metadata);
        return log;
    }
}
