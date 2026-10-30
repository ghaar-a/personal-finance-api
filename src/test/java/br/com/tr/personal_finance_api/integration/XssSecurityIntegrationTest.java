
package br.com.tr.personal_finance_api.integration;

import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import br.com.tr.personal_finance_api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class XssSecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String jwt;

    private static final String XSS_PAYLOAD =
            "<script>alert('xss')</script>"
                    + "<b>safe</b>"
                    + "<img src=x onerror=alert(1)>";

    @BeforeEach
    void setup() {
        String uniqueEmail = "xss-" + UUID.randomUUID() + "@test.com";

        User user = userRepository.save(
                User.builder()
                        .fullName("XSS Tester")
                        .email(uniqueEmail)
                        .passwordHash("unused-password-hash")
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build()
        );

        jwt = jwtService.generateToken(user.getId(), user.getEmail());
    }

    @Test
    @DisplayName("Endpoint vulnerável deve refletir HTML sem sanitização")
    void vulnerableReflectedXssShouldReturnRawInput() throws Exception {
        mockMvc.perform(get("/api/xss/reflect/vulnerable")
                        .header("Authorization", "Bearer " + jwt)
                        .param("input", XSS_PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(content().string(XSS_PAYLOAD));
    }

    @Test
    @DisplayName("Endpoint seguro deve sanitizar HTML refletido")
    void secureReflectedXssShouldSanitizeInput() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/api/xss/reflect/secure")
                                .header("Authorization", "Bearer " + jwt)
                                .param("input", XSS_PAYLOAD))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        assertTrue(!response.contains("<script"));
        assertTrue(!response.contains("onerror"));
        assertTrue(!response.contains("<img"));
        assertTrue(response.contains("<b>safe</b>"));
    }

    @Test
    @DisplayName("Endpoint vulnerável deve retornar conteúdo armazenado sem sanitização")
    void vulnerableStoredXssShouldReturnRawInput() throws Exception {
        mockMvc.perform(post("/api/xss/store/vulnerable")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(XSS_PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(content().string(XSS_PAYLOAD));
    }

    @Test
    @DisplayName("Endpoint seguro deve sanitizar conteúdo armazenado")
    void secureStoredXssShouldSanitizeInput() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/xss/store/secure")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.TEXT_PLAIN)
                                .content(XSS_PAYLOAD))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        assertTrue(!response.contains("<script"));
        assertTrue(!response.contains("onerror"));
        assertTrue(!response.contains("<img"));
        assertTrue(response.contains("<b>safe</b>"));
    }
}
