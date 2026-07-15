package com.slotcentral.auth.service;

import com.slotcentral.auth.domain.Employee;
import com.slotcentral.auth.dto.employee.*;
import com.slotcentral.auth.exception.ConflictException;
import com.slotcentral.auth.exception.ResourceNotFoundException;
import com.slotcentral.auth.exception.UnauthorizedException;
import com.slotcentral.auth.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        if (employeeRepository.existsByUid(request.uid())) {
            throw new ConflictException("Employee with uid '" + request.uid() + "' already exists");
        }
        Employee employee = new Employee();
        employee.setUid(request.uid());
        employee.setName(request.name());
        employee.setRole(request.role());
        employee.setPasswordHash(passwordEncoder.encode(request.password()));
        employee.setActive(true);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listEmployees() {
        return employeeRepository.findAll().stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(String uid) {
        return EmployeeResponse.from(findByUid(uid));
    }

    public EmployeeResponse updateEmployee(String uid, UpdateEmployeeRequest request) {
        Employee employee = findByUid(uid);
        if (request.name() != null) employee.setName(request.name());
        if (request.role() != null) employee.setRole(request.role());
        if (request.active() != null) employee.setActive(request.active());
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    public void deleteEmployee(String uid) {
        Employee employee = findByUid(uid);
        employeeRepository.delete(employee);
    }

    public EmployeeLoginResponse login(EmployeeLoginRequest request) {
        Employee employee = employeeRepository.findByUid(request.uid())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!employee.isActive()) {
            throw new UnauthorizedException("Account is inactive");
        }

        if (!passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtService.issueEmployeeToken(employee.getUid(), employee.getRole());
        return new EmployeeLoginResponse(
                token,
                employee.getUid(),
                employee.getRole().name(),
                jwtService.getEmployeeTokenTtlSeconds(),
                employee.isForcePasswordChange()
        );
    }

    private Employee findByUid(String uid) {
        return employeeRepository.findByUid(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + uid));
    }
}
