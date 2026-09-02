package com.inventory.system.repository;

import com.inventory.system.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    interface SalesAggregate {
        Long getProductId();
        String getSku();
        String getName();
        Long getTotalQuantitySold();
        java.math.BigDecimal getTotalRevenue();
    }

    @Query("""
            select si.product.id as productId, si.product.sku as sku, si.product.name as name,
                   sum(si.quantity) as totalQuantitySold, sum(si.subtotal) as totalRevenue
            from SaleItem si
            where si.sale.status = com.inventory.system.model.SaleStatus.COMPLETED
              and si.sale.saleDate between :from and :to
            group by si.product.id, si.product.sku, si.product.name
            order by sum(si.subtotal) desc
            """)
    List<SalesAggregate> findSalesAggregate(@Param("from") Instant from, @Param("to") Instant to);
}
