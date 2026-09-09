package com.example.rapiffy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

/**
 * ReturnItem — one line in a ReturnRequest.
 * References the original OrderItem so we have the price snapshot.
 */
@Entity
@Table(name = "return_items")
@Data
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    // Original order item being returned
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    // How many units the customer is returning (≤ orderItem.quantity)
    @Column(name = "quantity_to_return", nullable = false)
    private Integer quantityToReturn;

    // sellingPrice * quantityToReturn (calculated at submission time)
    @Column(name = "line_refund_amount", nullable = false)
    private Double lineRefundAmount;
}
