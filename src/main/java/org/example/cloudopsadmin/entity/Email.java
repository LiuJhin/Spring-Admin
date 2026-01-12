package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "emails")
@Data
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_internal_id", unique = true, nullable = false)
    private String emailInternalId;

    @Column(name = "email_address", unique = true, nullable = false)
    private String emailAddress;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String category; // normal, primary, secondary

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_email_id_fk")
    private Email parentEmail;

    @Column(name = "credit_card_last4")
    private String creditCardLast4;

    @Column(name = "is_sp_account")
    private Boolean isSpAccount = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id_fk")
    private Payer payer;

    @Column(name = "linked_account_name")
    private String linkedAccountName;

    @Column(name = "linked_account_uid")
    private String linkedAccountUid;

    @Column(name = "enable_forwarding")
    private Boolean enableForwarding = false;

    @Column(nullable = false)
    private String status = "active";

    @ElementCollection
    @CollectionTable(name = "email_labels", joinColumns = @JoinColumn(name = "email_id"))
    @Column(name = "label")
    private List<String> labels = new ArrayList<>();

    @OneToMany(mappedBy = "linkedEmail")
    private List<Account> accounts = new ArrayList<>();

    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
