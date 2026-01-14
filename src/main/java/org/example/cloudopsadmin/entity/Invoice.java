package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.example.cloudopsadmin.common.InvoiceStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_inv_customer_date", columnList = "customer_name,invoice_date")
})
@Data
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "tax_number")
    private String taxNumber;

    @Column(name = "payment_reference")
    private String paymentReference;

    @ElementCollection
    @CollectionTable(name = "invoice_activities", joinColumns = @JoinColumn(name = "invoice_id"))
    @Column(name = "activity")
    private List<String> activities = new ArrayList<>();

    @Column(name = "terms")
    private String terms;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineItem> items = new ArrayList<>();

    @Column(name = "subtotal_ex_tax")
    private Double subtotalExTax;

    @Column(name = "tax_total")
    private Double taxTotal;

    @Column(name = "grand_total")
    private Double grandTotal;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

