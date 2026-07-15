package com.slotcentral.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jti", nullable = false, unique = true, length = 100)
    private String jti;

    @Column(name = "subject", nullable = false, length = 100)
    private String subject;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    @PrePersist
    protected void onCreate() {
        revokedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
