package com.sheemab.shardedsagawallet.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransferRequestDto {

    private Long fromWalletId;
    private Long toWalletId;
    private BigDecimal amount;
    private String description;
}
