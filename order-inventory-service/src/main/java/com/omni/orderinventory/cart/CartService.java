package com.omni.orderinventory.cart;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.orderinventory.cart.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository itemRepo;

    public CartService(CartRepository cartRepo, CartItemRepository itemRepo) {
        this.cartRepo = cartRepo;
        this.itemRepo = itemRepo;
    }

    @Transactional
    public CartResponse getOrCreate(UUID customerId) {
        Cart cart = cartRepo.findByCustomerId(customerId).orElseGet(() -> {
            Cart c = new Cart();
            c.setCustomerId(customerId);
            return cartRepo.save(c);
        });
        return toResp(cart);
    }

    @Transactional
    public CartResponse addItem(UUID customerId, AddToCartRequest req) {
        Cart cart = cartRepo.findByCustomerId(customerId).orElseGet(() -> {
            Cart c = new Cart();
            c.setCustomerId(customerId);
            return cartRepo.save(c);
        });

        CartItem existing = itemRepo.findByCartIdAndSkuId(cart.getId(), req.skuId()).orElse(null);
        if (existing != null) {
            // Cộng dồn quantity
            existing.setQuantity(existing.getQuantity() + req.quantity());
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setSkuId(req.skuId());
            item.setQuantity(req.quantity());
            cart.getItems().add(item);
        }
        return toResp(cartRepo.save(cart));
    }

    @Transactional
    public CartResponse updateQuantity(UUID customerId, UUID skuId, UpdateQuantityRequest req) {
        Cart cart = cartRepo.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Cart không tồn tại"));
        CartItem item = itemRepo.findByCartIdAndSkuId(cart.getId(), skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SKU không có trong cart"));
        item.setQuantity(req.quantity());
        return toResp(cartRepo.save(cart));
    }

    @Transactional
    public CartResponse removeItem(UUID customerId, UUID skuId) {
        Cart cart = cartRepo.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        cart.getItems().removeIf(i -> i.getSkuId().equals(skuId));
        return toResp(cartRepo.save(cart));
    }

    @Transactional
    public void clear(UUID customerId) {
        cartRepo.findByCustomerId(customerId).ifPresent(c -> {
            c.getItems().clear();
            cartRepo.save(c);
        });
    }

    private CartResponse toResp(Cart cart) {
        var items = cart.getItems().stream()
                .map(i -> new CartItemResponse(i.getId(), i.getSkuId(), i.getQuantity()))
                .toList();
        return new CartResponse(cart.getId(), cart.getCustomerId(), items);
    }
}