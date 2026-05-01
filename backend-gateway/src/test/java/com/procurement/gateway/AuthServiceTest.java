package com.procurement.gateway;

import com.procurement.gateway.model.Employee;
import com.procurement.gateway.repository.EmployeeRepository;
import com.procurement.gateway.security.JwtUtil;
import com.procurement.gateway.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private Employee mockEmployee;

    @BeforeEach
    void setUp() {
        mockEmployee = new Employee();
        mockEmployee.setEmpId(1L);
        mockEmployee.setName("Test User");
        mockEmployee.setEmail("test@company.com");
        mockEmployee.setPassword("encodedPassword");
        mockEmployee.setRole(Employee.Role.EMPLOYEE);
    }

    @Test
    void login_WithValidCredentials_ReturnsToken() {
        when(employeeRepository.findByEmail("test@company.com"))
                .thenReturn(Optional.of(mockEmployee));
        when(passwordEncoder.matches("password123", "encodedPassword"))
                .thenReturn(true);
        when(jwtUtil.generateToken("test@company.com", "EMPLOYEE"))
                .thenReturn("mock-jwt-token");

        Map<String, Object> result = authService.login("test@company.com", "password123");

        assertNotNull(result);
        assertEquals("mock-jwt-token", result.get("token"));
        assertEquals("Test User", result.get("name"));
        assertEquals("EMPLOYEE", result.get("role"));
        verify(employeeRepository, times(1)).findByEmail("test@company.com");
    }

    @Test
    void login_WithInvalidEmail_ThrowsException() {
        when(employeeRepository.findByEmail("wrong@company.com"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                authService.login("wrong@company.com", "password123"));
    }

    @Test
    void login_WithWrongPassword_ThrowsException() {
        when(employeeRepository.findByEmail("test@company.com"))
                .thenReturn(Optional.of(mockEmployee));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword"))
                .thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                authService.login("test@company.com", "wrongpassword"));
    }

    @Test
    void register_WithExistingEmail_ThrowsException() {
        when(employeeRepository.existsByEmail("test@company.com"))
                .thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                authService.register("Test", "test@company.com",
                        "pass", "EMPLOYEE", null));
    }
}
