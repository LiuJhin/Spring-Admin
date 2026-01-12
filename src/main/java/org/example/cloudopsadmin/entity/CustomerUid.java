package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "customer_uids")
@Data
public class CustomerUid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uid", nullable = false)
    private String uid;

    @Column(name = "uid_type", nullable = false)
    private String uidType;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
