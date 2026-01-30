package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "cloud_providers")
@Data
public class CloudProvider {

    @Id
    @GeneratedValue(generator = "eight-digit-id-gen")
    @GenericGenerator(name = "eight-digit-id-gen", strategy = "org.example.cloudopsadmin.common.EightDigitIdGenerator")
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String status; // "Enabled", "Disabled"

    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
