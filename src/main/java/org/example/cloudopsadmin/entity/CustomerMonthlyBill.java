package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.example.cloudopsadmin.common.InvoiceStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_monthly_bills", indexes = {
        @Index(name = "idx_cmb_month_uid", columnList = "month,linked_account_uid"),
        @Index(name = "idx_cmb_month_customer", columnList = "month,customer_name")
})
@Data
public class CustomerMonthlyBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "month", nullable = false, length = 7)
    private String month;

    @Column(name = "cloud_vendor", nullable = false)
    private String cloudVendor;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "linked_account_uid", nullable = false)
    private String linkedAccountUid;

    @Column(name = "original_billing_percentage")
    private Double originalBillingPercentage;

    @Column(name = "total_bill")
    private Double totalBill;

    @Column(name = "undiscounted_bill")
    private Double undiscountedBill;

    @Column(name = "cost_discount_percentage")
    private Double costDiscountPercentage;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "customer_payable_bill")
    private Double customerPayableBill;

    @Column(name = "supplier_payable_bill")
    private Double supplierPayableBill;

    @Column(name = "profit")
    private Double profit;

    @Column(name = "is_invoiced")
    private Boolean isInvoiced;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_status")
    private InvoiceStatus invoiceStatus;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id_fk")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id_fk")
    private Customer customer;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
