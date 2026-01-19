package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {
    @Query(
            value = "SELECT i.payment_reference FROM invoices i WHERE i.payment_reference LIKE :pattern ORDER BY i.payment_reference DESC LIMIT 1",
            nativeQuery = true
    )
    Optional<String> findLastPaymentReference(@Param("pattern") String pattern);
}
