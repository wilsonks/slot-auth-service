package com.slotcentral.auth.controller;

import com.slotcentral.auth.dto.employee.*;
import com.slotcentral.auth.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FLOOR_MANAGER')")
    public ResponseEntity<List<EmployeeResponse>> listEmployees() {
        return ResponseEntity.ok(employeeService.listEmployees());
    }

    @GetMapping("/{uid}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLOOR_MANAGER')")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable String uid) {
        return ResponseEntity.ok(employeeService.getEmployee(uid));
    }

    @PutMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable String uid,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(uid, request));
    }

    @DeleteMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String uid) {
        employeeService.deleteEmployee(uid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<EmployeeLoginResponse> login(@Valid @RequestBody EmployeeLoginRequest request) {
        return ResponseEntity.ok(employeeService.login(request));
    }
}
