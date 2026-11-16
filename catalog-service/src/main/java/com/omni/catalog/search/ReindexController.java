
/**
 * Controller quản trị cung cấp API kích hoạt tái lập chỉ mục Elasticsearch thủ công.
 *
 * Chỉ cho phép tài khoản có quyền ADMIN thực thi để đồng bộ lại
 * dữ liệu tìm kiếm khi cần thiết.
 */

package com.omni.catalog.search;

import com.omni.common.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/catalog")
public class ReindexController {

    private final ReindexService reindexService;

    public ReindexController(ReindexService reindexService) {
        this.reindexService = reindexService;
    }

    /**
     * Rebuild toàn bộ index Elasticsearch từ dữ liệu gốc MongoDB.
     */
    @PostMapping("/admin/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Long>> reindex() {

        long count = reindexService.reindexAll();

        return ApiResponse.success(
                "Elasticsearch index rebuilt successfully",
                Map.of("reindexed", count)
        );
    }
}
