package com.omni.catalog.category;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "categories")
@Getter @Setter @NoArgsConstructor
public class Category {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String slug;

    private String parentId;   // null = root

    @Indexed
    private String status;     // ACTIVE, INACTIVE

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    public enum Status { ACTIVE, INACTIVE }
}