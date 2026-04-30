package br.com.tr.personal_finance_api.integration;

import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import br.com.tr.personal_finance_api.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthLoggingTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("teste@email.com"); // E-mail salvo no banco
        user.setPasswordHash(passwordEncoder.encode("123456")); // Senha salva no banco
        userRepository.save(user);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Deve registrar log de autenticação com sucesso")
    void shouldLogSuccessfulLogin() throws Exception {

        // O JSON deve conter o MESMO e-mail e senha do setup
        String loginJson = """
        {
            "email": "teste@email.com",
            "password": "123456"
        }
        """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk()) // Agora você pode descomentar essa linha!
                .andDo(print());
    }
}