
package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.application.service.AuthService;
import br.com.tr.personal_finance_api.interfaces.dto.AuthResponse;
import br.com.tr.personal_finance_api.interfaces.dto.LoginRequest;
import br.com.tr.personal_finance_api.interfaces.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String JWT_TOKEN = "test.jwt.token";
    private static final long JWT_EXPIRATION = 3_600_000L;

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);

        ReflectionTestUtils.setField(
                authController,
                "securityMode",
                "SECURE"
        );

        ReflectionTestUtils.setField(
                authController,
                "jwtExpiration",
                JWT_EXPIRATION
        );
    }

    @Test
    void registerShouldNotSetCookieInSecureMode() {
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@email.com",
                "password123"
        );

        AuthResponse authResponse = new AuthResponse(JWT_TOKEN);

        when(authService.register(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response =
                authController.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(authResponse, response.getBody());
        assertFalse(response.getHeaders().containsKey(HttpHeaders.SET_COOKIE));

        verify(authService).register(request);
    }

    @Test
    void registerShouldSetCsrfCookieInVulnerableMode() {
        ReflectionTestUtils.setField(
                authController,
                "securityMode",
                "VULNERABLE"
        );

        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@email.com",
                "password123"
        );

        AuthResponse authResponse = new AuthResponse(JWT_TOKEN);

        when(authService.register(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response =
                authController.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(authResponse, response.getBody());

        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        assertNotNull(cookie);
        assertTrue(cookie.contains("CSRF_DEMO_AUTH=" + JWT_TOKEN));
        assertTrue(cookie.contains("Path=/api/csrf"));
        assertTrue(cookie.contains("Max-Age=3600"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=None"));

        verify(authService).register(request);
    }

    @Test
    void loginShouldNotSetCookieInSecureMode() {
        LoginRequest request = new LoginRequest(
                "test@email.com",
                "password123"
        );

        HttpServletRequest httpRequest = new MockHttpServletRequest();

        AuthResponse authResponse = new AuthResponse(JWT_TOKEN);

        when(authService.login(request, httpRequest))
                .thenReturn(authResponse);

        ResponseEntity<AuthResponse> response =
                authController.login(request, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(authResponse, response.getBody());
        assertFalse(response.getHeaders().containsKey(HttpHeaders.SET_COOKIE));

        verify(authService).login(request, httpRequest);
    }

    @Test
    void loginShouldSetCsrfCookieInVulnerableMode() {
        ReflectionTestUtils.setField(
                authController,
                "securityMode",
                "VULNERABLE"
        );

        LoginRequest request = new LoginRequest(
                "test@email.com",
                "password123"
        );

        HttpServletRequest httpRequest = new MockHttpServletRequest();

        AuthResponse authResponse = new AuthResponse(JWT_TOKEN);

        when(authService.login(request, httpRequest))
                .thenReturn(authResponse);

        ResponseEntity<AuthResponse> response =
                authController.login(request, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(authResponse, response.getBody());

        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        assertNotNull(cookie);
        assertTrue(cookie.contains("CSRF_DEMO_AUTH=" + JWT_TOKEN));
        assertTrue(cookie.contains("Path=/api/csrf"));
        assertTrue(cookie.contains("Max-Age=3600"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=None"));

        verify(authService).login(request, httpRequest);
    }
}
