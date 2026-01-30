package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "partner_bds")
@Data
public class PartnerBd {

    @Id
    @GeneratedValue(generator = "eight-digit-id-gen")
    @GenericGenerator(name = "eight-digit-id-gen", strategy = "org.example.cloudopsadmin.common.EightDigitIdGenerator")
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;

    private String phone;

    @Column(nullable = false)
    private String status; // "Active", "Inactive"

    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
