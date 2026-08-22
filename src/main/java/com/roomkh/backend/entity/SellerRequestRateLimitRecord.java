package com.roomkh.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "seller_request_rate_limit_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerRequestRateLimitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false, length = 20)
    private SellerRequestRateLimitKeyType keyType;

    @Column(name = "key_hash", nullable = false, length = 255)
    private String keyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "window_type", nullable = false, length = 20)
    private SellerRequestRateLimitWindowType windowType;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    @Column(name = "window_started_at", nullable = false)
    private OffsetDateTime windowStartedAt;

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