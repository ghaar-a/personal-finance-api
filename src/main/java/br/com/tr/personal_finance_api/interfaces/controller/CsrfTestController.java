package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.application.service.TransferService;
import br.com.tr.personal_finance_api.interfaces.dto.TransferRequest;
import br.com.tr.personal_finance_api.interfaces.mapper.TransferMapper;
import br.com.tr.personal_finance_api.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/csrf")
@RequiredArgsConstructor
public class CsrfTestController {

    private final TransferService transferService;
    private final TransferMapper transferMapper;

    @PostMapping(
            value = "/transfer",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<Void> transfer(
            @Valid @ModelAttribute TransferRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        transferService.transfer(
                transferMapper.toCommand(
                        request,
                        user.getUserId()
                )
        );

        return ResponseEntity.ok().build();
    }
}