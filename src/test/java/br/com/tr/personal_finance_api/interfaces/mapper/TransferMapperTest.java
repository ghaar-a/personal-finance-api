
package br.com.tr.personal_finance_api.interfaces.mapper;

import br.com.tr.personal_finance_api.application.command.TransferCommand;
import br.com.tr.personal_finance_api.interfaces.dto.TransferRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransferMapperTest {

    private final TransferMapper mapper =
            Mappers.getMapper(TransferMapper.class);

    @Test
    void shouldMapTransferRequestAndAuthenticatedUserIdToCommand() {
        UUID userId = UUID.randomUUID();

        TransferRequest request = new TransferRequest(
                "ACC-100",
                "ACC-200",
                new BigDecimal("250.00"),
                "transfer-123"
        );

        TransferCommand command = mapper.toCommand(request, userId);

        assertEquals("ACC-100", command.fromAccount());
        assertEquals("ACC-200", command.toAccount());
        assertEquals(new BigDecimal("250.00"), command.amount());
        assertEquals("transfer-123", command.idempotencyKey());
        assertEquals(userId, command.userId());
    }

    @Test
    void shouldReturnNullWhenRequestAndUserIdAreNull() {
        assertNull(mapper.toCommand(null, null));
    }

    @Test
    void shouldMapAuthenticatedUserIdWhenRequestIsNull() {
        UUID userId = UUID.randomUUID();

        TransferCommand command = mapper.toCommand(null, userId);

        assertNotNull(command);
        assertNull(command.fromAccount());
        assertNull(command.toAccount());
        assertNull(command.amount());
        assertNull(command.idempotencyKey());
        assertEquals(userId, command.userId());
    }
}
docker compose down -v
export JWT_SECRET=$(openssl rand -base64 48)
docker compose up -d --build
sleep 40
docker compose ps
docker compose logs api | grep -iE "flyway|migrat|schema|validat|ERROR|Started|Caused" | tail -40
curl -s -o /dev/null -w "health: %{http_code}\n" localhost:8081/actuator/health
docker compose down