package com.sheemab.shardedsagawallet.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransferResponseDto {
    private Long sagaInstanceId;
}
