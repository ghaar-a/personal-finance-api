
package br.com.tr.personal_finance_api.integration;

import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import br.com.tr.personal_finance_api.interfaces.dto.AuthResponse;
import br.com.tr.personal_finance_api.interfaces.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class NoteControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwt;

    @BeforeEach
    void setUp() throws Exception {
        String email = "notes-" + UUID.randomUUID() + "@test.com";
        String password = "123456";

        userRepository.save(
                User.builder()
                        .fullName("Notes Tester")
                        .email(email)
                        .passwordHash(passwordEncoder.encode(password))
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build()
        );

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new LoginRequest(email, password)
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );

        jwt = authResponse.token();
    }

    @Test
    @DisplayName("Deve criar nota e retornar notas do usuário autenticado")
    void shouldCreateAndRetrieveAuthenticatedUserNotes() throws Exception {
        mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Minha primeira nota"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Minha primeira nota"))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.id").isNotEmpty());

        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Minha primeira nota"))
                .andExpect(jsonPath("$[0].userId").isNotEmpty());
    }

    @Test
    @DisplayName("Deve exigir autenticação para consultar notas")
    void shouldRequireAuthenticationToRetrieveNotes() throws Exception {
        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve exigir autenticação para criar nota")
    void shouldRequireAuthenticationToCreateNote() throws Exception {
        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Nota sem autenticação"))
                .andExpect(status().isUnauthorized());
    }
}
