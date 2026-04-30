package br.com.tr.personal_finance_api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AuthBruteForceTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve bloquear requisições após exceder o limite de tentativas (Rate Limit)")
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        String loginJson = """
                {
                    "username": "ataque@email.com",
                    "password": "senhaErrada123"
                }
                """;

        // Simulando o limite (ajuste conforme a config do seu Bucket4j, ex: 10 requisições)
        int limiteTentativas = 10;

        // 1. Loop de tentativas permitidas (Devem retornar 401 ou 200, não 429)
        for (int i = 0; i < limiteTentativas; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isUnauthorized()); // Aqui assume-se que a senha está errada
        }

        // 2. A tentativa que deve ser bloqueada (O ataque!)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isTooManyRequests()) // Assert HTTP 429
                .andDo(result -> {
                    String retryAfter = result.getResponse().getHeader("X-Rate-Limit-Retry-After-Seconds");
                    System.out.println("Ataque bloqueado! Tente novamente em: " + retryAfter + " segundos.");
                });
    }
}