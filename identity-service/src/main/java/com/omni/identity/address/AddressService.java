package com.omni.identity.address;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.identity.address.dto.AddressRequest;
import com.omni.identity.address.dto.AddressResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AddressService {
    private final AddressRepository repo;

    public AddressService(AddressRepository repo) {
        this.repo = repo;
    }

    /** Lấy danh sách địa chỉ, ưu tiên địa chỉ mặc định lên đầu */
    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        return repo.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    /** Lấy chi tiết địa chỉ thuộc sở hữu của user (Dùng cho Feign verify) */
    @Transactional(readOnly = true)
    public AddressResponse getOwned(UUID userId, UUID id) {
        Address a = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResponse(a);
    }

    /** Tạo địa chỉ mới, tự động set mặc định nếu là địa chỉ đầu tiên */
    @Transactional
    public AddressResponse create(UUID userId, AddressRequest req) {
        Address a = new Address();
        a.setUserId(userId);
        apply(a, req);

        if (req.isDefault() || repo.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).isEmpty()) {
            repo.clearDefault(userId);
            a.setDefault(true);
        }
        return toResponse(repo.save(a));
    }

    /** Cập nhật địa chỉ, xử lý logic clear mặc định cũ nếu có thay đổi */
    @Transactional
    public AddressResponse update(UUID userId, UUID id, AddressRequest req) {
        Address a = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        apply(a, req);
        if (req.isDefault()) {
            repo.clearDefault(userId);
            a.setDefault(true);
        }
        return toResponse(a);
    }

    /** Xóa địa chỉ (Chỉ xóa được địa chỉ của chính user đó) */
    @Transactional
    public void delete(UUID userId, UUID id) {
        Address a = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        repo.delete(a);
    }

    /** Helper: Copy dữ liệu từ Request DTO sang Entity */
    private void apply(Address a, AddressRequest req) {
        a.setRecipient(req.recipient());
        a.setPhone(req.phone());
        a.setProvince(req.province());
        a.setDistrict(req.district());
        a.setWard(req.ward());
        a.setDetail(req.detail());
    }

    /** Helper: Map Entity sang Response DTO */
    private AddressResponse toResponse(Address a) {
        return new AddressResponse(
                a.getId().toString(),
                a.getRecipient(),
                a.getPhone(),
                a.getProvince(),
                a.getDistrict(),
                a.getWard(),
                a.getDetail(),
                a.isDefault()
        );
    }
}