package com.omni.catalog.brand;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "brands")
@Getter @Setter @NoArgsConstructor
public class Brand {
    @Id private String id;
    private String name;
    @Indexed(unique = true) private String slug;
    private String logoUrl;
    @Indexed private String status;   // ACTIVE, INACTIVE
    private Instant createdAt;
    private Instant updatedAt;
    @Version private Long version;
}