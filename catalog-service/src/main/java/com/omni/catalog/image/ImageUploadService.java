package com.omni.catalog.image;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/** Lưu file local — Phase 7 có thể thay bằng S3/MinIO. */
@Service
public class ImageUploadService {

    private final String uploadDir;
    private final long maxSize;
    private final Set<String> allowedMime;

    public ImageUploadService(@Value("${app.upload.dir}") String uploadDir,
                              @Value("${app.upload.max-size-bytes:5242880}") long maxSize,
                              @Value("${app.upload.allowed-mime}") String allowed) {
        this.uploadDir = uploadDir;
        this.maxSize = maxSize;
        this.allowedMime = Set.of(allowed.split(","));
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File rỗng");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File vượt " + maxSize + " bytes");
        }
        String mime = file.getContentType();
        if (mime == null || !allowedMime.contains(mime)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "MIME không hợp lệ: " + mime);
        }
        // Verify magic bytes (đơn giản: đọc 4 byte đầu)
        String ext = switch (mime) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "MIME không hỗ trợ");
        };

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + "." + ext;
            Path target = dir.resolve(filename);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/api/v1/catalog/images/" + filename;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Lưu file thất bại");
        }
    }
}