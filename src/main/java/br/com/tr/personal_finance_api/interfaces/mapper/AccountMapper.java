
package br.com.tr.personal_finance_api.interfaces.mapper;

import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.interfaces.dto.AccountResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "accountNumber", source = "accountNumber")
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "status", source = "status")
    AccountResponse toResponse(Account account);
}
