package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_line_items")
@Data
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id_fk", nullable = false)
    private Invoice invoice;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "label")
    private String label;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "discount_pct")
    private Double discountPct;

    @Column(name = "tax_pct")
    private Double taxPct;

    @Column(name = "amount_ex_tax")
    private Double amountExTax;

    @Column(name = "amount_inc_tax")
    private Double amountIncTax;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

