package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.application.service.TransferService;
import br.com.tr.personal_finance_api.interfaces.dto.TransferRequest;
import br.com.tr.personal_finance_api.interfaces.mapper.TransferMapper;
import br.com.tr.personal_finance_api.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final TransferMapper transferMapper;

    @PostMapping
    public ResponseEntity<Void> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        transferService.transfer(
                transferMapper.toCommand(request, user.getUserId())
        );

        return ResponseEntity.ok().build();
    }
}