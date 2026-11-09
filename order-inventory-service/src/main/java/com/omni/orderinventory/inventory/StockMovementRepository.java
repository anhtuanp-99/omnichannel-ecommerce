package com.omni.orderinventory.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {}