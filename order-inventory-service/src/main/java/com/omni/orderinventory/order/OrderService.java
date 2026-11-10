package com.omni.orderinventory.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omni.common.dto.ApiResponse;
import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.orderinventory.cart.Cart;
import com.omni.orderinventory.cart.CartRepository;
import com.omni.orderinventory.integration.catalog.CatalogClient;
import com.omni.orderinventory.integration.catalog.dto.CatalogSkuDto;
import com.omni.orderinventory.integration.identity.IdentityClient;
import com.omni.orderinventory.integration.identity.dto.AddressDto;
import com.omni.orderinventory.inventory.ReservationService;
import com.omni.orderinventory.inventory.dto.ReservationResult;
import com.omni.orderinventory.order.dto.*;
import com.omni.orderinventory.order.idempotency.IdempotencyRecord;
import com.omni.orderinventory.order.idempotency.IdempotencyService;
import com.omni.orderinventory.order.idempotency.IdempotencyStatus;
import com.omni.orderinventory.outbox.OutboxEvent;
import com.omni.orderinventory.outbox.OutboxEventRepository;
import com.omni.orderinventory.saga.OrderCancellationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepo;
    private final OrderStatusHistoryRepository historyRepo;
    private final CartRepository cartRepo;
    private final ReservationService reservationService;
    private final CatalogClient catalogClient;
    private final IdentityClient identityClient;
    private final OutboxEventRepository outboxRepo;
    private final IdempotencyService idemService;
    private final OrderNumberGenerator numberGen;
    private final ObjectMapper om;
    private final OrderCancellationService cancellationService;
    private final TransactionTemplate transactionTemplate;

    public OrderService(OrderRepository orderRepo,
                        OrderStatusHistoryRepository historyRepo,
                        CartRepository cartRepo,
                        ReservationService reservationService,
                        CatalogClient catalogClient,
                        IdentityClient identityClient,
                        OutboxEventRepository outboxRepo,
                        IdempotencyService idemService,
                        OrderNumberGenerator numberGen,
                        ObjectMapper om,
                        @Lazy OrderCancellationService cancellationService,
                        TransactionTemplate transactionTemplate) {
        this.orderRepo = orderRepo;
        this.historyRepo = historyRepo;
        this.cartRepo = cartRepo;
        this.reservationService = reservationService;
        this.catalogClient = catalogClient;
        this.identityClient = identityClient;
        this.outboxRepo = outboxRepo;
        this.idemService = idemService;
        this.numberGen = numberGen;
        this.om = om;
        this.cancellationService = cancellationService;
        this.transactionTemplate = transactionTemplate;
    }

    public OrderResponse createOrder(UUID customerId, String idempotencyKey, CreateOrderRequest req) {
        String requestHash = idemService.hash(req);

        // 1. Fast path: Kiểm tra nếu request đã được xử lý trước đó (tránh tạo trùng)
        var existing = idemService.find(idempotencyKey);
        if (existing.isPresent()) {
            return handleExisting(existing.get(), requestHash);
        }

        // 2. Lấy giỏ hàng (Đã dùng @EntityGraph để lấy luôn items, tránh LazyInitialization)
        Cart cart = cartRepo.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Giỏ hàng rỗng"));
        if (cart.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Giỏ hàng rỗng");
        }

        // 3. Gọi Feign ra ngoài Transaction để không giữ DB Connection quá lâu
        ApiResponse<AddressDto> addressResp = identityClient.getAddress(req.addressId());
        if (addressResp == null || addressResp.getData() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Địa chỉ giao hàng không hợp lệ");
        }
        AddressDto address = addressResp.getData();

        List<DraftItem> draftItems = buildDraftItems(cart);

        // 4. Sử dụng TransactionTemplate để quản lý Transaction ngắn, tránh lỗi Self-invocation của Spring
        try {
            return transactionTemplate.execute(status -> doCreateInTx(customerId, idempotencyKey, requestHash, address, draftItems));
        } catch (DataIntegrityViolationException e) {
            // Trường hợp 2 request đến cùng lúc, request này bị lỗi UNIQUE constraint, lấy kết quả của request kia
            log.warn("Idempotency race condition key={}", idempotencyKey);
            var winner = idemService.find(idempotencyKey).orElseThrow(() -> e);
            return handleExisting(winner, requestHash);
        }
    }

    private OrderResponse doCreateInTx(UUID customerId, String idemKey, String requestHash,
                                       AddressDto address, List<DraftItem> draftItems) {
        IdempotencyRecord idem = idemService.markProcessing(idemKey, requestHash);

        Order order = new Order();
        order.setCode(numberGen.generate());
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);
        order.setCurrency("VND");
        order.setDiscount(0);
        order.setTax(0);
        order.setShippingFee(0);
        order.setShippingAddressSnapshot(toAddressMap(address));
        order.setContactSnapshot(Map.of("recipient", address.recipient(), "phone", address.phone()));

        long subtotal = 0;
        for (DraftItem d : draftItems) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setSkuId(d.skuId());
            oi.setSkuCode(d.skuCode());
            oi.setProductName(d.productName());
            oi.setAttributesSnapshot(d.attributes());
            oi.setUnitPrice(d.unitPrice());
            oi.setQuantity(d.quantity());
            oi.setLineTotal(d.unitPrice() * d.quantity());
            order.getItems().add(oi);
            subtotal += oi.getLineTotal();
        }
        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal);

        orderRepo.save(order);

        // Giữ tồn kho atomic — ném InsufficientStockException nếu thiếu hàng -> Rollback
        List<ReservationService.ItemDraft> reserveList = draftItems.stream()
                .map(d -> new ReservationService.ItemDraft(d.skuId(), d.quantity()))
                .toList();
        ReservationResult reservation = reservationService.reserveAll(order.getId(), reserveList);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(order.getId());
        history.setOldStatus(null);
        history.setNewStatus(OrderStatus.PENDING);
        history.setReason("Order created");
        history.setActor("system");
        historyRepo.save(history);

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("Order");
        event.setAggregateId(order.getId());
        event.setEventType("OrderCreated");
        event.setPayload(Map.of(
                "orderId", order.getId().toString(),
                "customerId", customerId.toString(),
                "totalAmount", order.getTotalAmount(),
                "currency", order.getCurrency(),
                "reservationExpiresAt", reservation.expiresAt().toString()
        ));
        outboxRepo.save(event);

        OrderResponse response = toResponse(order);

        // Chuyển Object Response thành String JSON để lưu vào DB
        try {
            String respJson = om.writeValueAsString(response);
            idemService.markCompleted(idem, respJson);
        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize OrderResponse", e);
            throw new RuntimeException("Lỗi serialize response", e);
        }

        return response;
    }

    private List<DraftItem> buildDraftItems(Cart cart) {
        return cart.getItems().stream().map(ci -> {
            CatalogSkuDto sku = catalogClient.getSku(ci.getSkuId().toString());
            if (!"ACTIVE".equalsIgnoreCase(sku.skuStatus()) || !"ACTIVE".equalsIgnoreCase(productStatus(sku))) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "SKU " + sku.code() + " hiện không còn kinh doanh");
            }
            return new DraftItem(ci.getSkuId(), sku.code(), sku.productName(),
                    sku.attributes() != null ? sku.attributes() : Map.of(),
                    sku.price(), ci.getQuantity());
        }).toList();
    }

    private String productStatus(CatalogSkuDto sku) {
        return sku.productStatus() != null ? sku.productStatus() : "";
    }

    /** Xử lý trả về kết quả cũ nếu request bị gửi trùng lặp */
    private OrderResponse handleExisting(IdempotencyRecord existing, String requestHash) {
        // Nếu dùng cùng Idempotency-Key nhưng payload khác -> Báo lỗi
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED, "Idempotency-Key đã dùng với dữ liệu khác");
        }

        // Nếu đã xử lý xong -> Đọc String JSON và parse ngược lại thành Object
        if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
            try {
                return om.readValue(existing.getResponseJson(), OrderResponse.class);
            } catch (JsonProcessingException e) {
                log.error("Lỗi parse dữ liệu Idempotency", e);
                throw new RuntimeException("Lỗi parse dữ liệu Idempotency", e);
            }
        }

        // Nếu đang xử lý (PROCESSING) -> Chặn không cho gửi tiếp
        throw new BusinessException(ErrorCode.STATE_CONFLICT, "Yêu cầu đang được xử lý, vui lòng không gửi lại liên tục");
    }

    public OrderResponse getOrder(UUID customerId, UUID orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!order.getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return toResponse(order);
    }

    public List<OrderSummaryResponse> listMyOrders(UUID customerId, int page, int size) {
        return orderRepo.findByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(page, Math.min(size, 50)))
                .stream()
                .map(o -> new OrderSummaryResponse(o.getId(), o.getCode(), o.getStatus().name(),
                        o.getTotalAmount(), o.getCurrency(), o.getCreatedAt()))
                .toList();
    }

    /** Delegate xử lý hủy đơn sang OrderCancellationService để đồng bộ logic. */
    @Transactional
    public void cancelPending(UUID customerId, UUID orderId, String reason) {
        cancellationService.cancel(customerId, orderId, reason, true);
    }

    private Map<String, Object> toAddressMap(AddressDto a) {
        return Map.of(
                "recipient", a.recipient(),
                "phone", a.phone(),
                "province", a.province(),
                "district", a.district(),
                "ward", a.ward(),
                "detail", a.detail()
        );
    }

    private OrderResponse toResponse(Order o) {
        var items = o.getItems().stream()
                .map(i -> new OrderItemResponse(i.getId(), i.getSkuId(), i.getSkuCode(),
                        i.getProductName(), i.getAttributesSnapshot(),
                        i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
                .toList();
        return new OrderResponse(o.getId(), o.getCode(), o.getCustomerId(), o.getStatus().name(),
                o.getSubtotal(), o.getDiscount(), o.getTax(), o.getShippingFee(),
                o.getTotalAmount(), o.getCurrency(),
                o.getShippingAddressSnapshot(), o.getContactSnapshot(),
                items, o.getCreatedAt(), o.getUpdatedAt());
    }

    public record DraftItem(UUID skuId, String skuCode, String productName,
                            Map<String, String> attributes, long unitPrice, int quantity) {}
}