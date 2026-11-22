package br.com.tr.personal_finance_api.security;

import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

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

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve ignorar rotas de autenticação")
    void shouldSkipAuthenticationRoutes() throws Exception {
        MockHttpServletRequest request = request("/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    @DisplayName("Deve continuar quando o header Authorization estiver ausente")
    void shouldContinueWhenAuthorizationHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = request("/api/accounts/secure/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userRepository);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Deve continuar quando o header não utilizar Bearer")
    void shouldContinueWhenAuthorizationHeaderIsNotBearer() throws Exception {
        MockHttpServletRequest request = request("/api/accounts/secure/1");
        request.addHeader("Authorization", "Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userRepository);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Deve autenticar usuário quando o token for válido")
    void shouldAuthenticateUserWhenTokenIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "valid-token";

        User user = createUser(userId, true);

        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = request("/api/accounts/secure/1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        var authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertInstanceOf(
                UsernamePasswordAuthenticationToken.class,
                authentication
        );

        CustomUserDetails principal =
                assertInstanceOf(CustomUserDetails.class, authentication.getPrincipal());

        assertEquals(userId, principal.getUserId());
        assertEquals("user@example.com", authentication.getName());
        assertTrue(principal.isEnabled());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals(Role.ROLE_USER.name())
                ));

        verify(jwtService).extractUserId(token);
        verify(userRepository).findById(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve continuar sem autenticação quando usuário não existir")
    void shouldContinueWhenUserDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "valid-token";

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("previous-user", null)
        );

        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        MockHttpServletRequest request = request("/api/accounts/secure/1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtService).extractUserId(token);
        verify(userRepository).findById(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Não deve autenticar usuário com conta desabilitada")
    void shouldRejectAuthenticationWhenUserIsDisabled() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "valid-token";

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("previous-user", null)
        );

        User user = createUser(userId, false);

        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = request("/api/accounts/secure/1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtService).extractUserId(token);
        verify(userRepository).findById(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Não deve autenticar usuário quando enabled for null")
    void shouldRejectAuthenticationWhenEnabledIsNull() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "valid-token";

        User user = createUser(userId, null);

        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = request("/api/accounts/secure/1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtService).extractUserId(token);
        verify(userRepository).findById(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve limpar o contexto quando a extração do token falhar")
    void shouldClearContextWhenTokenExtractionFails() throws Exception {
        String token = "invalid-token";

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("previous-user", null)
        );

        when(jwtService.extractUserId(token))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        MockHttpServletRequest request = request("/api/accounts/secure/1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtService).extractUserId(token);
        verifyNoInteractions(userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve limpar o contexto quando a consulta ao usuário falhar")
    void shouldClearContextWhenUserLookupFails() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "valid-token";

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("previous-user", null)
        );

        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(userRepository.findById(userId))
                .thenThrow(new IllegalStateException("Database unavailable"));

        MockHttpServletRequest request = request("/api/accounts/secure/1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtService).extractUserId(token);
        verify(userRepository).findById(userId);
        verify(filterChain).doFilter(request, response);
    }

    private User createUser(UUID userId, Boolean enabled) {
        return User.builder()
                .id(userId)
                .fullName("Test User")
                .email("user@example.com")
                .passwordHash("encoded-password")
                .role(Role.ROLE_USER)
                .enabled(enabled)
                .build();
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}