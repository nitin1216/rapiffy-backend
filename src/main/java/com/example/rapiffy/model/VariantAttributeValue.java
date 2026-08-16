package com.example.rapiffy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

/**
 * VariantAttributeValue stores the ACTUAL value for each attribute on a specific variant.
 *
 * Examples:
 *   ProductVariant "Nike Air Size 8 Red":
 *     → attributeType: "Size",   value: "8"
 *     → attributeType: "Colour", value: "Red"
 *
 *   ProductVariant "Shampoo 200ML":
 *     → attributeType: "Size",   value: "200 ML"
 *
 * Each variant will have as many VariantAttributeValues as there are attributeTypes on the parent product.
 */
@Entity
@Table(name = "variant_attribute_values")
@Data
public class VariantAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The attribute type this value belongs to (e.g. "Size", "Colour")
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_type_id", nullable = false)
    private VariantAttributeType attributeType;

    // The actual value (e.g. "8", "Red", "200 ML")
    @Column(name = "attribute_value", nullable = false)
    private String attributeValue;

    // Linked to ProductVariant (shop level) — nullable if master variant
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    // Linked to MasterProductVariant (catalog level) — nullable if shop variant
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_product_variant_id")
    private MasterProductVariant masterProductVariant;
}
