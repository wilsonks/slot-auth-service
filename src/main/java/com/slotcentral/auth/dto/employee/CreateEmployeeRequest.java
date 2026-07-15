package com.slotcentral.auth.dto.employee;

import com.slotcentral.auth.domain.EmployeeRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
    @NotBlank @Size(max = 100) String uid,
    @NotBlank @Size(max = 200) String name,
    @NotNull EmployeeRole role,
    @NotBlank @Size(min = 8) String password
) {}
