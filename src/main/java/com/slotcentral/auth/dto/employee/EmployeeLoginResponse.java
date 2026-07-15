package com.slotcentral.auth.dto.employee;

public record EmployeeLoginResponse(
    String token,
    String uid,
    String role,
    long expiresIn,
    boolean forcePasswordChange
) {}
