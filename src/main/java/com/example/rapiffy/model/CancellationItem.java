package com.example.rapiffy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

/**
 * CancellationItem — one line in a CancellationRequest.
 * Stores the item, quantity to cancel, and the calculated refund for that line.
 */
@Entity
@Table(name = "cancellation_items")
@Data
public class CancellationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancellation_request_id", nullable = false)
    private CancellationRequest cancellationRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = true)
    private OrderItem orderItem;

    // Snapshot — stored at request time so data is preserved even if OrderItem is deleted
    @Column(name = "product_name")
    private String productName;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "quantity_to_cancel", nullable = false)
    private Integer quantityToCancel;

    // sellingPrice * quantityToCancel (calculated at request time)
    @Column(name = "line_refund_amount", nullable = false)
    private Double lineRefundAmount;
}
