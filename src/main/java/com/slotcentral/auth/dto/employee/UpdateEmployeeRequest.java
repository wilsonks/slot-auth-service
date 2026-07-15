package com.slotcentral.auth.dto.employee;

import com.slotcentral.auth.domain.EmployeeRole;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeRequest(
    @Size(max = 200) String name,
    EmployeeRole role,
    Boolean active
) {}
