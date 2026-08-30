package com.example.rapiffy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Stores image URLs for a MasterProduct.
 * Multiple images allowed per product.
 * Images are physically stored on Serverbyt SFTP under /Images/products/{categoryId}/{productId}/
 */
@Entity
@Table(name = "master_product_images")
@Data
public class MasterProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_product_id", nullable = false)
    private MasterProduct masterProduct;

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
