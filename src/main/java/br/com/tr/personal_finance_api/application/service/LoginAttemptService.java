package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.domain.entity.LoginAttempt;
import br.com.tr.personal_finance_api.domain.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository repository;

    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_MINUTES = 15;

    public void recordSuccess(String email, String ip) {
        saveAttempt(email, true, ip);
    }

    public void recordFailure(String email, String ip) {
        saveAttempt(email, false, ip);
    }

    public boolean isBlocked(String email) {
        OffsetDateTime window = OffsetDateTime.now().minusMinutes(BLOCK_MINUTES);

        long failures = repository.countByEmailAndSuccessFalseAndAttemptTimeAfter(email, window);

        return failures >= MAX_ATTEMPTS;
    }

    private void saveAttempt(String email, boolean success, String ip) {
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .success(success)
                .ipAddress(ip)
                .attemptTime(OffsetDateTime.now())
                .build();

        repository.save(attempt);
    }
}