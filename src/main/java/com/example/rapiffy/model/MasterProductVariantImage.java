package com.example.rapiffy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Stores image URLs for a MasterProductVariant.
 * Multiple images allowed per variant.
 * Images are physically stored on Serverbyt SFTP under /Images/variants/{categoryId}/{productId}/{variantId}/
 */
@Entity
@Table(name = "master_product_variant_images")
@Data
public class MasterProductVariantImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private MasterProductVariant variant;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    // Controls the order images are displayed on frontend
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
