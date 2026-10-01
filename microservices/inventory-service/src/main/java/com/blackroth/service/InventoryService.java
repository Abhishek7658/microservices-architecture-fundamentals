package com.blackroth.inventory_service.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blackroth.inventory_service.model.Inventory;
import com.blackroth.inventory_service.repository.InventoryRepository;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public boolean reserveInventory(Long productId, Integer quantity) {

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() ->
                        new RuntimeException("Inventory not found for product: " + productId));

        if (inventory.getAvailableQuantity() < quantity) {
            return false;
        }

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() - quantity
        );

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() + quantity
        );

        inventoryRepository.save(inventory);

        return true;
    }

    @Transactional
    public void releaseInventory(Long productId, Integer quantity) {

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() ->
                        new RuntimeException("Inventory not found for product: " + productId));

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() - quantity
        );

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() + quantity
        );

        inventoryRepository.save(inventory);
    }
}