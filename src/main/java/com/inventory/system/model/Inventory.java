package com.inventory.system.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Stock levels for one Product. Never mutate quantityOnHand directly
 * outside of a service method that also writes a StockMovement row —
 * that log is the only source of truth for how the number got here.
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Min(0)
    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand;

    @Min(0)
    @Column(name = "reorder_level", nullable = false)
    @Builder.Default
    private Integer reorderLevel = 0;

    @Min(0)
    @Column(name = "max_stock_level")
    private Integer maxStockLevel;

    /**
     * Optimistic-locking token. A second, concurrent write to the same
     * inventory row throws ObjectOptimisticLockingFailureException, which
     * the global exception handler turns into a 409 conflict rather than
     * silently letting one update clobber the other.
     */
    @Version
    private Long version;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private Instant lastUpdated;

    public boolean isLowStock() {
        return quantityOnHand != null && reorderLevel != null && quantityOnHand <= reorderLevel;
    }
}
