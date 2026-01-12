package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.Payer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PayerRepository extends JpaRepository<Payer, Long>, JpaSpecificationExecutor<Payer> {
    Optional<Payer> findByPayerId(String payerId);
    Optional<Payer> findByPayerInternalId(String payerInternalId);
    boolean existsByPayerId(String payerId);
}
