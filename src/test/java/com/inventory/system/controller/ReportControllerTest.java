package com.inventory.system.controller;

import com.inventory.system.dto.SalesReportItem;
import com.inventory.system.dto.SalesReportResponse;
import com.inventory.system.dto.StockReportItem;
import com.inventory.system.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer unit tests for {@link ReportController} with a mocked service.
 */
@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    void stockReport_returns200WithItems() throws Exception {
        when(reportService.getStockReport()).thenReturn(List.of(
                new StockReportItem(1L, "SKU-001", "Widget", 8, 10, true)));

        mockMvc.perform(get("/api/reports/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$[0].lowStock").value(true));
    }

    @Test
    void lowStockReport_returns200WithItems() throws Exception {
        when(reportService.getLowStockReport()).thenReturn(List.of(
                new StockReportItem(1L, "SKU-001", "Widget", 2, 10, true)));

        mockMvc.perform(get("/api/reports/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantityOnHand").value(2));
    }

    @Test
    void salesReport_returns200_whenDatesValid() throws Exception {
        SalesReportResponse response = new SalesReportResponse(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                List.of(new SalesReportItem(1L, "SKU-001", "Widget", 37L, BigDecimal.valueOf(2921.63))),
                BigDecimal.valueOf(2921.63));
        when(reportService.getSalesReport(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(response);

        mockMvc.perform(get("/api/reports/sales")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(2921.63))
                .andExpect(jsonPath("$.items[0].totalQuantitySold").value(37));
    }

    @Test
    void salesReport_returns400_whenDateMissing() throws Exception {
        mockMvc.perform(get("/api/reports/sales").param("from", "2026-09-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void salesReport_returns400_whenDateMalformed() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("from", "not-a-date")
                        .param("to", "2026-09-30"))
                .andExpect(status().isBadRequest());
    }
}
