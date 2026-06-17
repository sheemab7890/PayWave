package com.sheemab.shardedsagawallet.entities;


import com.sheemab.shardedsagawallet.enums.SagaStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.*;



@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "saga_instances")
@Builder
public class SagaInstance {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SagaStatus status = SagaStatus.STARTED;

    // just store as plain String
    @Column(name = "context", columnDefinition = "json")
    private String context;

    @Column(name = "current_step")
    private String currentStep;

}
