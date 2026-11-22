package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import br.com.tr.personal_finance_api.interfaces.dto.AuthResponse;
import br.com.tr.personal_finance_api.interfaces.dto.LoginRequest;
import br.com.tr.personal_finance_api.interfaces.dto.RegisterRequest;
import br.com.tr.personal_finance_api.interfaces.exception.TooManyRequestsException;
import br.com.tr.personal_finance_api.interfaces.exception.UnauthorizedException;
import br.com.tr.personal_finance_api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .fullName("Test User")
                .email("user@example.com")
                .passwordHash("encoded-password")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();
    }

    @Test
    void shouldRegisterUserAndReturnGeneratedToken() {
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "user@example.com",
                "plain-password"
        );

        when(passwordEncoder.encode("plain-password"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);
                    savedUser.setId(userId);
                    return savedUser;
                });

        when(jwtService.generateToken(userId, "user@example.com"))
                .thenReturn("generated-token");

        AuthResponse response = authService.register(request);

        assertEquals("generated-token", response.token());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("Test User", savedUser.getFullName());
        assertEquals("user@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPasswordHash());
        assertEquals(Role.ROLE_USER, savedUser.getRole());
        assertTrue(savedUser.getEnabled());

        verify(passwordEncoder).encode("plain-password");
        verify(jwtService).generateToken(userId, "user@example.com");
    }

    @Test
    void shouldLoginSuccessfullyUsingForwardedIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("correct-password", "encoded-password"))
                .thenReturn(true);

        when(jwtService.generateToken(userId, "user@example.com"))
                .thenReturn("valid-token");

        AuthResponse response = authService.login(
                new LoginRequest("user@example.com", "correct-password"),
                request
        );

        assertEquals("valid-token", response.token());

        verify(loginAttemptService).recordSuccess(
                "user@example.com",
                "203.0.113.10"
        );
        verify(loginAttemptService, never()).recordFailure(anyString(), anyString());
        verify(auditLogService, never()).logFailedLogin(anyString(), anyString());
        verify(auditLogService, never()).logBlockedLogin(anyString(), anyString());
        verify(jwtService).generateToken(userId, "user@example.com");
    }

    @Test
    void shouldRejectLoginWhenAccountIsDisabled() {
        user.setEnabled(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.30");

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("correct-password", "encoded-password"))
                .thenReturn(true);

        when(loginAttemptService.isBlocked("user@example.com"))
                .thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(
                        new LoginRequest("user@example.com", "correct-password"),
                        request
                )
        );

        assertEquals("Invalid credentials", exception.getMessage());

        verify(loginAttemptService).recordFailure(
                "user@example.com",
                "192.0.2.30"
        );
        verify(auditLogService).logFailedLogin(
                "user@example.com",
                "192.0.2.30"
        );
        verify(loginAttemptService).isBlocked("user@example.com");

        verify(loginAttemptService, never()).recordSuccess(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(), anyString());
    }

    @Test
    void shouldRejectLoginWhenAccountEnabledStateIsNull() {
        user.setEnabled(null);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("correct-password", "encoded-password"))
                .thenReturn(true);

        when(loginAttemptService.isBlocked("user@example.com"))
                .thenReturn(false);

        assertThrows(
                UnauthorizedException.class,
                () -> authService.login(
                        new LoginRequest("user@example.com", "correct-password"),
                        null
                )
        );

        verify(loginAttemptService).recordFailure(
                "user@example.com",
                "unknown"
        );
        verify(auditLogService).logFailedLogin(
                "user@example.com",
                "unknown"
        );
        verify(loginAttemptService, never()).recordSuccess(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(), anyString());
    }

    @Test
    void shouldRejectLoginWhenEmailIsNull() {
        LoginRequest loginRequest = new LoginRequest(null, "password");

        assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest, null)
        );

        verifyNoInteractions(userRepository, loginAttemptService, auditLogService);
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void shouldRejectLoginWhenEmailIsBlank() {
        LoginRequest loginRequest = new LoginRequest("   ", "password");

        assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest, null)
        );

        verifyNoInteractions(userRepository, loginAttemptService, auditLogService);
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void shouldRejectLoginWhenPasswordIsNull() {
        LoginRequest loginRequest = new LoginRequest("user@example.com", null);

        assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest, null)
        );

        verifyNoInteractions(userRepository, loginAttemptService, auditLogService);
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void shouldRejectLoginWhenPasswordIsBlank() {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "   ");

        assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest, null)
        );

        verifyNoInteractions(userRepository, loginAttemptService, auditLogService);
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void shouldRecordFailureWhenUserDoesNotExist() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.15");

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        when(loginAttemptService.isBlocked("unknown@example.com"))
                .thenReturn(false);

        assertThrows(
                UnauthorizedException.class,
                () -> authService.login(
                        new LoginRequest("unknown@example.com", "password"),
                        request
                )
        );

        verify(loginAttemptService).recordFailure(
                "unknown@example.com",
                "192.0.2.15"
        );
        verify(auditLogService).logFailedLogin(
                "unknown@example.com",
                "192.0.2.15"
        );
        verify(loginAttemptService).isBlocked("unknown@example.com");

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void shouldRecordFailureWhenPasswordIsIncorrect() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.20");

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        when(loginAttemptService.isBlocked("user@example.com"))
                .thenReturn(false);

        assertThrows(
                UnauthorizedException.class,
                () -> authService.login(
                        new LoginRequest("user@example.com", "wrong-password"),
                        request
                )
        );

        verify(loginAttemptService).recordFailure(
                "user@example.com",
                "192.0.2.20"
        );
        verify(auditLogService).logFailedLogin(
                "user@example.com",
                "192.0.2.20"
        );
        verify(loginAttemptService).isBlocked("user@example.com");

        verify(jwtService, never()).generateToken(any(), anyString());
    }

    @Test
    void shouldThrowTooManyRequestsWhenUnknownUserIsBlocked() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        when(loginAttemptService.isBlocked("unknown@example.com"))
                .thenReturn(true);

        assertThrows(
                TooManyRequestsException.class,
                () -> authService.login(
                        new LoginRequest("unknown@example.com", "password"),
                        null
                )
        );

        verify(loginAttemptService).recordFailure(
                "unknown@example.com",
                "unknown"
        );
        verify(auditLogService).logFailedLogin(
                "unknown@example.com",
                "unknown"
        );
        verify(auditLogService).logBlockedLogin(
                "unknown@example.com",
                "unknown"
        );
    }

    @Test
    void shouldThrowTooManyRequestsWhenPasswordFailureCausesBlock() {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        when(loginAttemptService.isBlocked("user@example.com"))
                .thenReturn(true);

        assertThrows(
                TooManyRequestsException.class,
                () -> authService.login(
                        new LoginRequest("user@example.com", "wrong-password"),
                        null
                )
        );

        verify(loginAttemptService).recordFailure(
                "user@example.com",
                "unknown"
        );
        verify(auditLogService).logFailedLogin(
                "user@example.com",
                "unknown"
        );
        verify(auditLogService).logBlockedLogin(
                "user@example.com",
                "unknown"
        );
    }

    @Test
    void shouldUseRemoteAddressWhenForwardedHeaderIsAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.25");

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        when(loginAttemptService.isBlocked("user@example.com"))
                .thenReturn(false);

        assertThrows(
                UnauthorizedException.class,
                () -> authService.login(
                        new LoginRequest("user@example.com", "wrong-password"),
                        request
                )
        );

        verify(loginAttemptService).recordFailure(
                "user@example.com",
                "198.51.100.25"
        );
    }

    @Test
    void shouldUseRemoteAddressWhenForwardedHeaderIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.30");
        request.addHeader("X-Forwarded-For", "   ");

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        when(loginAttemptService.isBlocked("unknown@example.com"))
                .thenReturn(false);

        assertThrows(
                UnauthorizedException.class,
                () -> authService.login(
                        new LoginRequest("unknown@example.com", "password"),
                        request
                )
        );

        verify(loginAttemptService).recordFailure(
                "unknown@example.com",
                "198.51.100.30"
        );
    }
}