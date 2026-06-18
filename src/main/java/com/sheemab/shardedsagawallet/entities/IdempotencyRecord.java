package com.sheemab.shardedsagawallet.entities;


import com.sheemab.shardedsagawallet.enums.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key")
        // unique constraint = DB-level guard against concurrent duplicates
)
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;
    // The client-generated UUID — this IS the primary key
    // No auto-generated ID needed here

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdempotencyStatus status;

    @Column(name = "saga_instance_id")
    private Long sagaInstanceId;
    // Stored after saga completes — returned to client on duplicate requests

    @Column(nullable = false)
    private LocalDateTime createdAt;
    // Used for stale PROCESSING recovery and cleanup
}
