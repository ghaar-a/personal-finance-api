
package br.com.tr.personal_finance_api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@AutoConfigureMockMvc
class SecurityHeadersIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Resposta HTTP deve conter Content-Security-Policy")
    void shouldReturnContentSecurityPolicy() throws Exception {
        mockMvc.perform(get("/api/auth/unknown"))
                .andExpect(header().string(
                        "Content-Security-Policy",
                        "default-src 'self'; "
                                + "script-src 'self'; "
                                + "style-src 'self' 'unsafe-inline'; "
                                + "img-src 'self' data:; "
                                + "font-src 'self'; "
                                + "connect-src 'self'; "
                                + "frame-ancestors 'none';"
                ));
    }

    @Test
    @DisplayName("Resposta HTTP deve conter Strict-Transport-Security")
    void shouldReturnStrictTransportSecurity() throws Exception {
        mockMvc.perform(get("/api/auth/unknown"))
                .andExpect(header().string(
                        "Strict-Transport-Security",
                        "max-age=31536000; includeSubDomains; preload"
                ));
    }

    @Test
    @DisplayName("Resposta HTTP deve impedir MIME sniffing")
    void shouldReturnXContentTypeOptions() throws Exception {
        mockMvc.perform(get("/api/auth/unknown"))
                .andExpect(header().string(
                        "X-Content-Type-Options",
                        "nosniff"
                ));
    }

    @Test
    @DisplayName("Resposta HTTP deve impedir framing")
    void shouldReturnXFrameOptions() throws Exception {
        mockMvc.perform(get("/api/auth/unknown"))
                .andExpect(header().string(
                        "X-Frame-Options",
                        "DENY"
                ));
    }

    @Test
    @DisplayName("Resposta HTTP deve conter Referrer-Policy")
    void shouldReturnReferrerPolicy() throws Exception {
        mockMvc.perform(get("/api/auth/unknown"))
                .andExpect(header().string(
                        "Referrer-Policy",
                        "no-referrer"
                ));
    }

    @Test
    @DisplayName("Resposta HTTP deve conter X-XSS-Protection")
    void shouldReturnXXssProtection() throws Exception {
        mockMvc.perform(get("/api/auth/unknown"))
                .andExpect(header().string(
                        "X-XSS-Protection",
                        "1; mode=block"
                ));
    }
}
