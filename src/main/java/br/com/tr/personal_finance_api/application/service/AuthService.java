package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import br.com.tr.personal_finance_api.interfaces.dto.*;
import br.com.tr.personal_finance_api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Senha inválida");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token);
    }
}