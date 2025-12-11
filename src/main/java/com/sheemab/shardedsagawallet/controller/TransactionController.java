package com.sheemab.shardedsagawallet.controller;

import com.sheemab.shardedsagawallet.dtos.TransferRequestDto;
import com.sheemab.shardedsagawallet.dtos.TransferResponseDto;
import com.sheemab.shardedsagawallet.entities.Transaction;
import com.sheemab.shardedsagawallet.services.TransactionService;
import com.sheemab.shardedsagawallet.services.TransferSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransferSagaService transferSagaService;

   @PostMapping
    public ResponseEntity<TransferResponseDto> createTransaction(@RequestBody TransferRequestDto requestDto){
         try
            {
                Long sagaInstanceId = transferSagaService.initiateTransfer(
                        requestDto.getFromWalletId(),
                        requestDto.getToWalletId(),
                        requestDto.getAmount(),
                        requestDto.getDescription()
                );

                return ResponseEntity.status(HttpStatus.CREATED).body(
                        TransferResponseDto.builder()
                                .sagaInstanceId(sagaInstanceId)
                                .build()
                );
            }
            catch (Exception e)
            {
                log.info("Error initiating transfer saga: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

   }

}
