package br.com.tr.personal_finance_api.security.mode;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SecureAccountService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> findAccountByNumber(String accountNumber) {

        // ✔️ CORRETO: prepared statement
        String sql = "SELECT * FROM accounts WHERE account_number = ?";

        return jdbcTemplate.queryForList(sql, accountNumber);
    }
}