package br.com.tr.personal_finance_api.interfaces.mapper;

import br.com.tr.personal_finance_api.application.command.TransferCommand;
import br.com.tr.personal_finance_api.interfaces.dto.TransferRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "fromAccount", source = "request.fromAccount")
    @Mapping(target = "toAccount", source = "request.toAccount")
    @Mapping(target = "amount", source = "request.amount")
    @Mapping(target = "idempotencyKey", source = "request.idempotencyKey")
    @Mapping(target = "userId", source = "userId")
    TransferCommand toCommand(TransferRequest request, UUID userId);
}