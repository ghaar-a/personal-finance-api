
package br.com.tr.personal_finance_api.integration;

import br.com.tr.personal_finance_api.application.service.AuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security-mode=VULNERABLE")
class CsrfProtectionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private AuditLogService auditLogService;

    @Test
    @DisplayName("Modo vulnerável deve rejeitar POST sem token CSRF")
    void shouldRejectPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verify(auditLogService).logCsrfAttempt(
                anyString(),
                eq("/api/auth/login"),
                eq("POST")
        );
    }

    @Test
    @DisplayName("Modo vulnerável deve permitir POST com token CSRF válido")
    void shouldAllowPostWithValidCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .header("Origin", "http://localhost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertNotEquals(
                                403,
                                result.getResponse().getStatus(),
                                "A requisição com token CSRF válido não deve ser bloqueada pelo CSRF"
                        )
                );
    }
}
