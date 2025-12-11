package com.sheemab.shardedsagawallet.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DebitWalletRequestDto {

    private BigDecimal amount;
}
