package com.sheemab.shardedsagawallet.dtos;

import com.sheemab.shardedsagawallet.entities.Wallet;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserResponseDto {
    private Long id;
    private String name;
    private String email;
    private BigDecimal balance;
}
