package br.com.tr.personal_finance_api.config;

import br.com.tr.personal_finance_api.interfaces.exception.SecurityExceptionHandler;
import br.com.tr.personal_finance_api.security.CorrelationIdFilter;
import br.com.tr.personal_finance_api.security.CsrfAttackLoggingFilter;
import br.com.tr.personal_finance_api.security.JwtAuthFilter;
import br.com.tr.personal_finance_api.security.RateLimitFilter;
import br.com.tr.personal_finance_api.security.SecurityHeadersFilter;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.HeaderWriterFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityExceptionHandler securityExceptionHandler;
    private final CorrelationIdFilter correlationIdFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final RateLimitFilter rateLimitFilter;
    private final JwtAuthFilter jwtAuthFilter;
    private final CsrfAttackLoggingFilter csrfAttackLoggingFilter;

    @Value("${app.security-mode:SECURE}")
    private String securityMode;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/actuator/**").permitAll();

                    if ("SECURE".equalsIgnoreCase(securityMode)) {
                        auth
                                .requestMatchers(
                                        "/api/accounts/vulnerable/**",
                                        "/api/csrf/**"
                                ).denyAll();
                    }

                    auth
                            .requestMatchers("/api/**").hasRole("USER")
                            .anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler)
                )

                // Disponibiliza o correlation ID para toda a cadeia de segurança.
                .addFilterBefore(
                        correlationIdFilter,
                        SecurityContextHolderFilter.class
                )

                // Executa depois dos headers padrão e antes dos filtros de negócio.
                .addFilterAfter(
                        securityHeadersFilter,
                        HeaderWriterFilter.class
                );

        if ("VULNERABLE".equalsIgnoreCase(securityMode)) {
            http
                    .csrf(csrf ->
                            csrf.ignoringRequestMatchers(request ->
                                    "POST".equalsIgnoreCase(request.getMethod())
                                            && "/api/csrf/transfer".equals(
                                            request.getRequestURI()
                                                    .substring(
                                                            request.getContextPath().length()
                                                    )
                                    )
                            )
                    )
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(
                                    SessionCreationPolicy.IF_REQUIRED
                            )
                    )

                    // Registra tentativas antes de aplicar o rate limit.
                    .addFilterAfter(
                            csrfAttackLoggingFilter,
                            SecurityHeadersFilter.class
                    )
                    .addFilterAfter(
                            rateLimitFilter,
                            CsrfAttackLoggingFilter.class
                    )
                    .addFilterBefore(
                            jwtAuthFilter,
                            UsernamePasswordAuthenticationFilter.class
                    );

        } else if ("SECURE".equalsIgnoreCase(securityMode)) {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(
                                    SessionCreationPolicy.STATELESS
                            )
                    )
                    .addFilterAfter(
                            rateLimitFilter,
                            SecurityHeadersFilter.class
                    )
                    .addFilterBefore(
                            jwtAuthFilter,
                            UsernamePasswordAuthenticationFilter.class
                    );

        } else {
            throw new IllegalStateException(
                    "Unsupported app.security-mode: " + securityMode
                            + ". Expected VULNERABLE or SECURE."
            );
        }

        return http.build();
    }

    /*
     * Os filtros são executados exclusivamente pela SecurityFilterChain.
     * Isso impede que o Spring Boot também os registre no servlet container.
     */

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        return disabledServletRegistration(correlationIdFilter);
    }

    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilterRegistration() {
        return disabledServletRegistration(securityHeadersFilter);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        return disabledServletRegistration(rateLimitFilter);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration() {
        return disabledServletRegistration(jwtAuthFilter);
    }

    @Bean
    public FilterRegistrationBean<CsrfAttackLoggingFilter> csrfAttackLoggingFilterRegistration() {
        return disabledServletRegistration(csrfAttackLoggingFilter);
    }

    private <T extends Filter> FilterRegistrationBean<T> disabledServletRegistration(
            T filter
    ) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}