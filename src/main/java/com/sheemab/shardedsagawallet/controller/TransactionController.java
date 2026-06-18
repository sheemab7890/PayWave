package com.sheemab.shardedsagawallet.controller;

import com.sheemab.shardedsagawallet.dtos.TransferRequestDto;
import com.sheemab.shardedsagawallet.dtos.TransferResponseDto;
import com.sheemab.shardedsagawallet.exception.DuplicateRequestException;
import com.sheemab.shardedsagawallet.exception.ShardUnavailableException;
import com.sheemab.shardedsagawallet.services.TransferSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransferSagaService transferSagaService;

    @PostMapping
    public ResponseEntity<TransferResponseDto> createTransaction(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody TransferRequestDto requestDto
    ) {
        try {
            Long sagaInstanceId = transferSagaService.initiateTransfer(
                    idempotencyKey,
                    requestDto.getFromWalletId(),
                    requestDto.getToWalletId(),
                    requestDto.getAmount(),
                    requestDto.getDescription()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(TransferResponseDto.builder()
                            .sagaInstanceId(sagaInstanceId)
                            .build());

        } catch (DuplicateRequestException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409

        } catch (ShardUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build(); // 503

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }
}
