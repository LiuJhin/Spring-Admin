package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "credit_cards")
@Data
public class CreditCard {

    @Id
    @GeneratedValue(generator = "eight-digit-id-gen")
    @GenericGenerator(name = "eight-digit-id-gen", strategy = "org.example.cloudopsadmin.common.EightDigitIdGenerator")
    private Long id;

    @NotBlank(message = "Bank name is required")
    @JsonProperty("bank_name")
    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @NotBlank(message = "Holder name is required")
    @JsonProperty("card_holder")
    @Column(name = "holder_name", nullable = false)
    private String holderName;

    @NotBlank(message = "First 4 digits are required")
    @JsonProperty("first_4_digits")
    @Column(name = "first_4_digits", nullable = false)
    private String firstFourDigits;

    @NotBlank(message = "Last 4 digits are required")
    @JsonProperty("last_4_digits")
    @Column(name = "last_4_digits", nullable = false)
    private String lastFourDigits;

    @JsonProperty("expiry_date")
    @Column(name = "expiration_date")
    private String expirationDate; // e.g., "MM/YY"

    @NotBlank(message = "Status is required")
    @Column(nullable = false)
    private String status; // "Active", "Inactive"

    private String description;

    @Transient // Not stored in DB, calculated on fly or separate logic
    private Integer linkedAccountCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
