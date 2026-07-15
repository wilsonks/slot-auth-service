package com.slotcentral.auth.repository;

import com.slotcentral.auth.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUid(String uid);
    boolean existsByUid(String uid);
}
