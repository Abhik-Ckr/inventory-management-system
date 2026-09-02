package com.inventory.system.repository;

import com.inventory.system.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByInventoryIdOrderByTimestampDesc(Long inventoryId);
}
