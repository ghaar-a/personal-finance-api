package br.com.tr.personal_finance_api.config;

import br.com.tr.personal_finance_api.interfaces.exception.SecurityExceptionHandler;
import br.com.tr.personal_finance_api.security.CsrfAttackLoggingFilter;
import br.com.tr.personal_finance_api.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityExceptionHandler securityExceptionHandler;
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
                    .addFilterBefore(
                            jwtAuthFilter,
                            UsernamePasswordAuthenticationFilter.class
                    )
                    .addFilterBefore(
                            csrfAttackLoggingFilter,
                            CsrfFilter.class
                    );

        } else if ("SECURE".equalsIgnoreCase(securityMode)) {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(
                                    SessionCreationPolicy.STATELESS
                            )
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

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}