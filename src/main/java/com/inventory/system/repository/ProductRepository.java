package com.inventory.system.repository;

import com.inventory.system.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
    java.util.List<Product> findByActiveTrue();

    /**
     * Case-insensitive partial match over name and SKU, active products only —
     * lets a client resolve a human search term ("keyboard") to product ids.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
                   OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :term, '%')))
            ORDER BY p.name ASC
            """)
    java.util.List<Product> search(@Param("term") String term);
}
