package com.sheemab.shardedsagawallet.entities;

import com.sheemab.shardedsagawallet.enums.TransactionStatus;
import com.sheemab.shardedsagawallet.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transaction")
public class Transaction {

    @Id
    private Long id;

    @Column(name = "to_wallet_id")
    private Long toWalletId;

    @Column(name = "from_wallet_id")
    private Long fromWalletId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private TransactionType type = TransactionType.TRANSFER;

    @Column(name = "description")
    private String description;

    @Column(name = "saga_instance_id")
    private Long sagaInstanceId;
}
