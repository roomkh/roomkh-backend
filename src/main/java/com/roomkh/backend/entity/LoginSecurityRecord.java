package com.roomkh.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "login_security_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginSecurityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false, length = 20)
    private LoginSecurityKeyType keyType;

    @Column(name = "key_hash", nullable = false, length = 255)
    private String keyHash;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "window_started_at", nullable = false)
    private OffsetDateTime windowStartedAt;

    @Column(name = "last_failed_at")
    private OffsetDateTime lastFailedAt;

    @Column(name = "blocked_until")
    private OffsetDateTime blockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}