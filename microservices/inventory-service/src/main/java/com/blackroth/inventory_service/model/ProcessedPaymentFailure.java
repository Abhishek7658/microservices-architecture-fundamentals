package com.blackroth.inventory_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "processed_payment_failures",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_processed_payment_failure_order",
                        columnNames = "order_id"
                )
        }
)
public class ProcessedPaymentFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    public ProcessedPaymentFailure() {
    }

    public ProcessedPaymentFailure(Long orderId) {
        this.orderId = orderId;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}