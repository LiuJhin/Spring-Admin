package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_payments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"month", "customer_name"})
})
@Data
public class MonthlyPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 7)
    private String month; // yyyy-MM

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "received_amount")
    private Double receivedAmount = 0.0;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "remarks")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
