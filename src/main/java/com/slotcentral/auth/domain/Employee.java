package com.slotcentral.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uid", nullable = false, unique = true, length = 100)
    private String uid;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private EmployeeRole role;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "force_password_change", nullable = false)
    private boolean forcePasswordChange = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public EmployeeRole getRole() { return role; }
    public void setRole(EmployeeRole role) { this.role = role; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isForcePasswordChange() { return forcePasswordChange; }
    public void setForcePasswordChange(boolean forcePasswordChange) { this.forcePasswordChange = forcePasswordChange; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
