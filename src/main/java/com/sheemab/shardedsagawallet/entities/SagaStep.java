package com.sheemab.shardedsagawallet.entities;

import com.sheemab.shardedsagawallet.enums.StepStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "saga_steps")
public class SagaStep {

    @Id
    private Long id;

    @Column(name = "saga_instance_id", nullable = false)
    private Long sagaInstanceId;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StepStatus status;

    @Column(name = "error_message", nullable = true)
    private String errorMessage;

    @Column(name = "step_data", columnDefinition = "json")
    private String stepData;

    public void markAsCompleted() {
        this.status = StepStatus.COMPLETED;
    }

    public void markAsFailed(){
        this.status = StepStatus.FAILED;
    }

    public void markAsPending() {
        this.status = StepStatus.PENDING;
    }

    public void markAsRunning() {
        this.status = StepStatus.RUNNING;
    }

    public void markAsCompensated() {
        this.status = StepStatus.COMPENSATED;
    }

    public void markAsCompensating() {
        this.status = StepStatus.COMPENSATING;
    }

}
