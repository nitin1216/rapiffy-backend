package com.example.rapiffy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

/**
 * VariantAttributeType defines WHAT dimensions a product varies on.
 *
 * Examples:
 *   MasterProduct "Nike Air" → attributeTypes: ["Size", "Colour"]
 *   MasterProduct "Shampoo"  → attributeTypes: ["Size"]
 *   MasterProduct "T-Shirt"  → attributeTypes: ["Size", "Colour", "Material"]
 *
 * Each VariantAttributeType belongs to ONE MasterProduct.
 * ShopProduct inherits these from MasterProduct OR defines its own (for unlisted products).
 */
@Entity
@Table(name = "variant_attribute_types")
@Data
public class VariantAttributeType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The attribute name — e.g. "Size", "Colour", "Material"
    @Column(name = "attribute_name", nullable = false)
    private String attributeName;

    // Display order — e.g. Size first, then Colour
    @Column(name = "display_order")
    private Integer displayOrder;

    // Linked to MasterProduct (nullable for unlisted shop products)
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_product_id")
    private MasterProduct masterProduct;

    // Linked to ShopProduct (nullable for catalog products)
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_product_id")
    private ShopProduct shopProduct;
}
