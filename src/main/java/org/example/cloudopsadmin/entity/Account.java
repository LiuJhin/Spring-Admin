package org.example.cloudopsadmin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_internal_id", unique = true, nullable = false)
    private String accountInternalId;

    @Column(name = "uid", unique = true, nullable = false)
    private String uid;

    @Column(name = "monitor_email", nullable = false)
    private String monitorEmail;

    @Column(name = "monitor_url", nullable = false)
    private String monitorUrl;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "mfa_status")
    private String mfaStatus;

    @Column(name = "account_source", nullable = false)
    private String accountSource;

    @Column(name = "account_attribution", nullable = false)
    private String accountAttribution;

    @Column(name = "is_monitored_sp", nullable = false)
    private Boolean isMonitoredSp;

    @Column(name = "monitor_bill_group", nullable = false)
    private String monitorBillGroup;

    @Column(name = "send_po")
    private Boolean sendPo;

    @Column(name = "bound_credit_card_encrypted", nullable = false, length = 2048)
    private String boundCreditCardEncrypted;

    @Column(name = "bound_credit_card_masked", nullable = false)
    private String boundCreditCardMasked;

    @Column(name = "bound_email", nullable = false)
    private String boundEmail;

    @Column(name = "risk_discount", nullable = false)
    private Double riskDiscount;

    @Column(name = "cost_discount", nullable = false)
    private Double costDiscount;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "is_submitted", nullable = false)
    private Boolean isSubmitted = false;

    @ElementCollection
    @CollectionTable(name = "account_labels", joinColumns = @JoinColumn(name = "account_id"))
    @Column(name = "label")
    private List<String> labels = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    @JsonIgnore
    @ToString.Exclude
    private Payer payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id_fk")
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id_fk")
    @JsonIgnore
    @ToString.Exclude
    private Email linkedEmail;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
