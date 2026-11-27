
package br.com.tr.personal_finance_api.security.mode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountSqlInjectionServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SecureAccountService secureAccountService;

    @Test
    void secureServiceShouldUseParameterizedQuery() {
        String accountNumber = "ACC-001";

        secureAccountService.findAccountByNumber(accountNumber);

        verify(jdbcTemplate).queryForList(
                "SELECT * FROM accounts WHERE account_number = ?",
                accountNumber
        );
    }

    @Test
    void vulnerableServiceShouldConcatenateAccountNumberIntoQuery() {
        VulnerableAccountService vulnerableAccountService =
                new VulnerableAccountService(jdbcTemplate);

        String accountNumber = "ACC-001";

        vulnerableAccountService.findAccountByNumber(accountNumber);

        verify(jdbcTemplate).queryForList(
                "SELECT * FROM accounts WHERE account_number = 'ACC-001'"
        );
    }
}
