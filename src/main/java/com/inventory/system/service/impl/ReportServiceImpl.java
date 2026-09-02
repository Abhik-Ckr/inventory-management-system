package com.inventory.system.service.impl;

import com.inventory.system.dto.SalesReportItem;
import com.inventory.system.dto.SalesReportResponse;
import com.inventory.system.dto.StockReportItem;
import com.inventory.system.model.Inventory;
import com.inventory.system.repository.InventoryRepository;
import com.inventory.system.repository.SaleItemRepository;
import com.inventory.system.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final InventoryRepository inventoryRepository;
    private final SaleItemRepository saleItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StockReportItem> getStockReport() {
        return inventoryRepository.findAllForActiveProducts().stream()
                .map(this::toStockReportItem)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockReportItem> getLowStockReport() {
        return inventoryRepository.findLowStock().stream()
                .map(this::toStockReportItem)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<SalesReportItem> items = saleItemRepository.findSalesAggregate(fromInstant, toInstant).stream()
                .map(agg -> new SalesReportItem(
                        agg.getProductId(), agg.getSku(), agg.getName(),
                        agg.getTotalQuantitySold(), agg.getTotalRevenue()))
                .toList();

        BigDecimal totalRevenue = items.stream()
                .map(SalesReportItem::totalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SalesReportResponse(from, to, items, totalRevenue);
    }

    private StockReportItem toStockReportItem(Inventory inventory) {
        return new StockReportItem(
                inventory.getProduct().getId(),
                inventory.getProduct().getSku(),
                inventory.getProduct().getName(),
                inventory.getQuantityOnHand(),
                inventory.getReorderLevel(),
                inventory.isLowStock());
    }
}
