package com.procurement.gateway.service;

import com.procurement.gateway.model.Employee;
import com.procurement.gateway.repository.EmployeeRepository;
import com.procurement.gateway.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public Map<String, Object> login(String email, String password) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, employee.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(email, employee.getRole().name());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("empId", employee.getEmpId());
        response.put("name", employee.getName());
        response.put("email", employee.getEmail());
        response.put("role", employee.getRole().name());
        if (employee.getDepartment() != null) {
            response.put("department", employee.getDepartment().getName());
        }
        return response;
    }

    public Map<String, Object> register(String name, String email,
                                        String password, String role, Long deptId) {
        if (employeeRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        Employee employee = new Employee();
        employee.setName(name);
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode(password));
        employee.setRole(Employee.Role.valueOf(role));
        employeeRepository.save(employee);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("empId", employee.getEmpId());
        return response;
    }
}
