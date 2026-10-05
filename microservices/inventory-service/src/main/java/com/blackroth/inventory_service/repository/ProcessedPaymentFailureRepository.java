package com.blackroth.inventory_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blackroth.inventory_service.model.ProcessedPaymentFailure;

public interface ProcessedPaymentFailureRepository
        extends JpaRepository<ProcessedPaymentFailure, Long> {

    Optional<ProcessedPaymentFailure> findByOrderId(Long orderId);
}