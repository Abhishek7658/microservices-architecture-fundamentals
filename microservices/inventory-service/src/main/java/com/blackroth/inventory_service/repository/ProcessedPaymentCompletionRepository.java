package com.blackroth.inventory_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blackroth.inventory_service.model.ProcessedPaymentCompletion;

public interface ProcessedPaymentCompletionRepository
        extends JpaRepository<ProcessedPaymentCompletion, Long> {

    Optional<ProcessedPaymentCompletion> findByOrderId(Long orderId);
}