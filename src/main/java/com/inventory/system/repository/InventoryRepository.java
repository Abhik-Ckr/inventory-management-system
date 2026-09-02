package com.inventory.system.repository;

import com.inventory.system.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    // Comparing two columns of the same row can't be expressed as a derived
    // query method, hence the explicit JPQL.
    @Query("select i from Inventory i where i.quantityOnHand <= i.reorderLevel")
    List<Inventory> findLowStock();

    @Query("select i from Inventory i join i.product p where p.active = true")
    List<Inventory> findAllForActiveProducts();
}
