package com.omni.orderinventory.cart;

import com.omni.orderinventory.cart.dto.*;
import com.omni.orderinventory.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    /** API: Lấy hoặc tạo mới giỏ hàng của user */
    @GetMapping
    public CartResponse get(CurrentUser user) {
        return service.getOrCreate(user.userId());
    }

    /** API: Thêm sản phẩm vào giỏ hàng */
    @PostMapping("/items")
    public CartResponse addItem(CurrentUser user,
                                @Valid @RequestBody AddToCartRequest req) {
        return service.addItem(user.userId(), req);
    }

    /** API: Cập nhật số lượng sản phẩm trong giỏ */
    @PutMapping("/items/{skuId}")
    public CartResponse updateQty(CurrentUser user, @PathVariable UUID skuId,
                                  @Valid @RequestBody UpdateQuantityRequest req) {
        return service.updateQuantity(user.userId(), skuId, req);
    }

    /** API: Xóa một sản phẩm khỏi giỏ */
    @DeleteMapping("/items/{skuId}")
    public CartResponse remove(CurrentUser user, @PathVariable UUID skuId) {
        return service.removeItem(user.userId(), skuId);
    }

    /** API: Xóa toàn bộ giỏ hàng */
    @DeleteMapping
    public void clear(CurrentUser user) {
        service.clear(user.userId());
    }
}