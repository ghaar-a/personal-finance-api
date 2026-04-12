package br.com.tr.personal_finance_api.security.mode;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VulnerableAccountService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> findAccountByNumber(String accountNumber) {

        // ❌ ERRO GRAVE: concatenação direta (SQL Injection)
        String sql = "SELECT * FROM accounts WHERE account_number = '" + accountNumber + "'";

        return jdbcTemplate.queryForList(sql);
    }
}