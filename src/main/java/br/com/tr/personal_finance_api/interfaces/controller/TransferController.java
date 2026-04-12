package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.application.service.TransferService;
import br.com.tr.personal_finance_api.interfaces.dto.TransferRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {

        transferService.transfer(request);

        return ResponseEntity.ok().build();
    }
}