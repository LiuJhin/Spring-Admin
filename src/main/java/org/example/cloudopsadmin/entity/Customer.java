package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_internal_id", unique = true, nullable = false)
    private String customerInternalId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "email", nullable = false)
    private String email;

    // Assuming payer_id refers to the payer's business ID string, not the database primary key ID.
    // However, to maintain integrity, we might want to link to the Payer entity.
    // The request payload has "payer_id": "payer_20260108_003", which looks like our internal ID format.
    // Let's store the string for now, or better, map it to the Payer entity if possible.
    // Given the previous Payer entity uses `payerId` (length 12) and `payerInternalId`.
    // The request example "payer_20260108_003" looks like an internal ID.
    // I will add a ManyToOne relationship to Payer using `payerInternalId` as the join key logic, 
    // but JPA usually joins on PK. I'll store the `payerId` string provided in the request
    // and optionally link to the Payer entity if needed. 
    // For this request, I'll store the `payerId` string as provided and also try to link it.
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id_fk")
    private Payer payer;

    @ElementCollection
    @CollectionTable(name = "customer_permissions", joinColumns = @JoinColumn(name = "customer_id"))
    @Column(name = "permission")
    private List<String> permissions = new ArrayList<>();

    @Column(name = "original_billing_percentage")
    private Double originalBillingPercentage;

    @ElementCollection
    @CollectionTable(name = "customer_products", joinColumns = @JoinColumn(name = "customer_id"))
    @Column(name = "product")
    private List<String> productsUsed = new ArrayList<>();

    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    @Column(name = "company")
    private String company;

    @Column(name = "status")
    private String status;

    @ElementCollection
    @CollectionTable(name = "customer_labels", joinColumns = @JoinColumn(name = "customer_id"))
    @Column(name = "label")
    private List<String> labels = new ArrayList<>();

    @Column(name = "remarks")
    private String remarks;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerUid> uids = new ArrayList<>();

    @OneToMany(mappedBy = "customer")
    private List<Account> accounts = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
