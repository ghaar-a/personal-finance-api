package br.com.tr.personal_finance_api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security-mode=VULNERABLE")
class AccountIdorVulnerableModeIntegrationTest
        extends AbstractAccountIdorIntegrationTest {

    @Test
    @DisplayName("Modo vulnerável deve demonstrar IDOR sem expor campos internos")
    void vulnerableEndpointShouldExposeIdorScenarioWithoutLeakingInternalFields()
            throws Exception {

        mockMvc.perform(get("/api/accounts/vulnerable/" + accountId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.accountNumber").value("ACC-100"))
                .andExpect(jsonPath("$.balance").value(1000))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());
    }
}