package com.omni.orderinventory.inventory;

import com.omni.orderinventory.inventory.dto.*;
import com.omni.orderinventory.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/stock")
@PreAuthorize("hasAnyRole('ADMIN','INVENTORY_STAFF')")
public class StockAdminController {

    private final StockAdminService service;

    public StockAdminController(StockAdminService service) {
        this.service = service;
    }

    /** API: Xem tồn kho của SKU */
    @GetMapping("/{skuId}")
    public StockResponse get(@PathVariable UUID skuId) {
        return service.get(skuId);
    }

    /** API: Nhập thêm hàng vào kho */
    @PostMapping("/import")
    public StockResponse importStock(CurrentUser user,
                                     @Valid @RequestBody StockImportRequest req) {
        return service.importStock(req, user.userId());
    }

    /** API: Điều chỉnh số lượng tồn kho (Admin/Staff) */
    @PostMapping("/{skuId}/adjust")
    public StockResponse adjust(CurrentUser user, @PathVariable UUID skuId,
                                @Valid @RequestBody AdjustStockRequest req) {
        return service.adjust(skuId, req, user.userId());
    }
}