package com.slotcentral.auth.dto.employee;

import jakarta.validation.constraints.NotBlank;

public record EmployeeLoginRequest(
    @NotBlank String uid,
    @NotBlank String password
) {}
