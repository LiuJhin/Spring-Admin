package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "operation_logs", indexes = {
        @Index(name = "idx_oplog_target", columnList = "targetType,targetId"),
        @Index(name = "idx_oplog_operator", columnList = "operatorEmail")
})
@Data
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String operatorEmail;

    @Column(nullable = false)
    private String operatorName;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false)
    private String targetId;

    @Column(length = 1024)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

