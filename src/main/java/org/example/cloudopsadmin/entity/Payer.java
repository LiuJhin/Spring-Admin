package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payers")
@Data
public class Payer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payer_internal_id", unique = true, nullable = false)
    private String payerInternalId;

    @Column(name = "payer_name", nullable = false)
    private String payerName;

    @Column(name = "payer_id", unique = true, nullable = false, length = 12)
    private String payerId;

    @Column(name = "signin_url", nullable = false)
    private String signinUrl;

    @Column(name = "iam_username", nullable = false)
    private String iamUsername;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "remarks")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "payer", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Account> accounts = new java.util.ArrayList<>();
}
