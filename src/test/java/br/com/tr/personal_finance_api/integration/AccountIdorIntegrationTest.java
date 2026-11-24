package br.com.tr.personal_finance_api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AccountIdorIntegrationTest extends AbstractAccountIdorIntegrationTest {

    @Test
    @DisplayName("Endpoint seguro deve impedir acesso à conta de outro usuário")
    void secureEndpointShouldBlockIdor() throws Exception {

        mockMvc.perform(get("/api/accounts/secure/" + accountId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Modo seguro deve bloquear o endpoint vulnerável")
    void secureModeShouldBlockVulnerableEndpoint() throws Exception {

        mockMvc.perform(get("/api/accounts/vulnerable/" + accountId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }
}