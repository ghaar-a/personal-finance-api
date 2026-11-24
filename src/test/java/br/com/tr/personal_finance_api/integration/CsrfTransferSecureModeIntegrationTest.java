package br.com.tr.personal_finance_api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CsrfTransferSecureModeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Modo seguro deve bloquear endpoint demonstrativo mesmo para usuário autenticado")
    void shouldBlockCsrfDemoEndpointForAuthenticatedUser()
            throws Exception {

        mockMvc.perform(
                        post("/api/csrf/transfer")
                                .with(user("authenticated-user").roles("USER"))
                                .contentType(
                                        MediaType.APPLICATION_FORM_URLENCODED
                                )
                                .param("fromAccount", "SOURCE")
                                .param("toAccount", "DESTINATION")
                                .param("amount", "100.00")
                )
                .andExpect(status().isForbidden());
    }
}