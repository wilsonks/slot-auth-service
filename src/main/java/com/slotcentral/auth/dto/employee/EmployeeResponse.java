package com.slotcentral.auth.dto.employee;

import com.slotcentral.auth.domain.Employee;
import com.slotcentral.auth.domain.EmployeeRole;
import java.time.Instant;

public record EmployeeResponse(
    Long id,
    String uid,
    String name,
    EmployeeRole role,
    boolean active,
    boolean forcePasswordChange,
    Instant createdAt,
    Instant updatedAt
) {
    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(
            e.getId(), e.getUid(), e.getName(), e.getRole(),
            e.isActive(), e.isForcePasswordChange(),
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
