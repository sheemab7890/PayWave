package com.sheemab.shardedsagawallet.entities;


import com.sheemab.shardedsagawallet.enums.SagaStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import org.apache.calcite.model.JsonType;
import lombok.*;



@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "saga_instances")
@Builder
public class SagaInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
//    @Builder.Default
    private SagaStatus status = SagaStatus.STARTED;

    @Type(JsonType.class)
    @Column(name = "context", columnDefinition = "json")
    private String context;

    @Column(name = "current_step")
    private String currentStep;

}
