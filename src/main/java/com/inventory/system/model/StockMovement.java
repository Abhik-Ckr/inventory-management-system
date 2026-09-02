package com.inventory.system.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Append-only record of every stock change — restock, sale, or manual
 * adjustment. This is what makes stock-level history and sales reports
 * possible without recomputing anything after the fact.
 */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @Min(1)
    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 255)
    private String reason;

    /** External reference, e.g. "SALE-1042" or a purchase-order number. */
    @Column(length = 100)
    private String reference;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private Instant timestamp;
}
