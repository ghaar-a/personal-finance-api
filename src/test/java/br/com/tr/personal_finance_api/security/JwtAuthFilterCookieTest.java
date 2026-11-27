package br.com.tr.personal_finance_api.security;

import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterCookieTest {

    private static final String CSRF_TRANSFER_PATH = "/api/csrf/transfer";
    private static final String TOKEN = "valid-cookie-token";

    private JwtService jwtService;
    private UserRepository userRepository;
    private JwtAuthFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userRepository = mock(UserRepository.class);
        filterChain = mock(FilterChain.class);

        filter = new JwtAuthFilter(jwtService, userRepository);

        ReflectionTestUtils.setField(filter, "securityMode", "VULNERABLE");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve autenticar usando cookie no POST demonstrativo em modo vulnerável")
    void shouldAuthenticateUsingDemoCookieOnVulnerableCsrfTransferPost() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);

        when(jwtService.extractUserId(TOKEN)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = request("POST", CSRF_TRANSFER_PATH);
        request.setCookies(new Cookie(CsrfDemoCookie.NAME, TOKEN));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertEquals("user@example.com", authentication.getName());

        verify(jwtService).extractUserId(TOKEN);
        verify(userRepository).findById(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Não deve aceitar cookie demonstrativo no modo seguro")
    void shouldIgnoreDemoCookieInSecureMode() throws Exception {
        ReflectionTestUtils.setField(filter, "securityMode", "SECURE");

        MockHttpServletRequest request = request("POST", CSRF_TRANSFER_PATH);
        request.setCookies(new Cookie(CsrfDemoCookie.NAME, TOKEN));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Não deve aceitar cookie demonstrativo em requisição GET")
    void shouldIgnoreDemoCookieForGetRequest() throws Exception {
        MockHttpServletRequest request = request("GET", CSRF_TRANSFER_PATH);
        request.setCookies(new Cookie(CsrfDemoCookie.NAME, TOKEN));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Não deve aceitar cookie demonstrativo em outra rota")
    void shouldIgnoreDemoCookieForDifferentPath() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/accounts/secure/1");
        request.setCookies(new Cookie(CsrfDemoCookie.NAME, TOKEN));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve continuar sem autenticação quando o cookie estiver ausente")
    void shouldContinueWhenDemoCookieIsMissing() throws Exception {
        MockHttpServletRequest request = request("POST", CSRF_TRANSFER_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve continuar sem autenticação quando não encontrar o cookie demonstrativo")
    void shouldContinueWhenDemoCookieIsNotPresentAmongCookies() throws Exception {
        MockHttpServletRequest request = request("POST", CSRF_TRANSFER_PATH);
        request.setCookies(
                new Cookie("OTHER_COOKIE", "other-value"),
                new Cookie("ANOTHER_COOKIE", "another-value")
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Header Authorization deve ter precedência sobre o cookie demonstrativo")
    void shouldNotFallbackToCookieWhenAuthorizationHeaderIsPresent() throws Exception {
        MockHttpServletRequest request = request("POST", CSRF_TRANSFER_PATH);
        request.addHeader("Authorization", "Basic invalid-credentials");
        request.setCookies(new Cookie(CsrfDemoCookie.NAME, TOKEN));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userRepository);
        verify(filterChain).doFilter(request, response);
    }

    private User createUser(UUID userId) {
        return User.builder()
                .id(userId)
                .fullName("Test User")
                .email("user@example.com")
                .passwordHash("encoded-password")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(path);
        return request;
    }
}
