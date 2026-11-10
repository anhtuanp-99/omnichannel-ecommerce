package com.omni.orderinventory.inventory;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.orderinventory.common.OrderInventoryConstants;
import com.omni.orderinventory.inventory.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StockAdminService {

    private final StockRepository stockRepo;
    private final StockMovementRepository movementRepo;

    public StockAdminService(StockRepository stockRepo, StockMovementRepository movementRepo) {
        this.stockRepo = stockRepo;
        this.movementRepo = movementRepo;
    }

    @Transactional
    public StockResponse importStock(StockImportRequest req, UUID actorId) {
        UUID wh = OrderInventoryConstants.DEFAULT_WAREHOUSE_ID;
        Stock stock = stockRepo.findBySkuIdAndWarehouseId(req.skuId(), wh)
                .orElseGet(() -> {
                    Stock s = new Stock();
                    s.setSkuId(req.skuId());
                    s.setWarehouseId(wh);
                    s.setOnHand(0);
                    s.setReserved(0);
                    return s;
                });

        stock.setOnHand(stock.getOnHand() + req.quantity());
        stockRepo.save(stock);

        StockMovement mv = new StockMovement();
        mv.setSkuId(req.skuId());
        mv.setWarehouseId(wh);
        mv.setQuantity(req.quantity());
        mv.setType(StockMovementType.IMPORT);
        mv.setReferenceType("IMPORT");
        mv.setReferenceId(req.reason() != null ? req.reason() : "manual");
        mv.setActor("staff:" + actorId);
        movementRepo.save(mv);

        return toResp(stock);
    }

    @Transactional
    public StockResponse adjust(UUID skuId, AdjustStockRequest req, UUID actorId) {
        UUID wh = OrderInventoryConstants.DEFAULT_WAREHOUSE_ID;
        Stock stock = stockRepo.findBySkuIdAndWarehouseId(skuId, wh)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        long newOnHand = stock.getOnHand() + req.delta();
        if (newOnHand < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "on_hand không thể âm (delta=" + req.delta() + ")");
        }
        if (newOnHand < stock.getReserved()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "on_hand sau điều chỉnh < reserved (invariant available >= 0)");
        }
        stock.setOnHand(newOnHand);
        stockRepo.save(stock);

        StockMovement mv = new StockMovement();
        mv.setSkuId(skuId);
        mv.setWarehouseId(wh);
        mv.setQuantity(req.delta());
        mv.setType(StockMovementType.ADJUST);
        mv.setReferenceType("ADJUST");
        mv.setReferenceId(req.reason());
        mv.setActor("staff:" + actorId);
        movementRepo.save(mv);

        return toResp(stock);
    }

    public StockResponse get(UUID skuId) {
        Stock s = stockRepo.findBySkuIdAndWarehouseId(skuId, OrderInventoryConstants.DEFAULT_WAREHOUSE_ID)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResp(s);
    }

    private StockResponse toResp(Stock s) {
        return new StockResponse(s.getId(), s.getSkuId(), s.getWarehouseId(),
                s.getOnHand(), s.getReserved(), s.getAvailable(), s.getVersion());
    }
}