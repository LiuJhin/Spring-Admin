package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByCustomerInternalId(String customerInternalId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCustomerNameIgnoreCase(String customerName);
    
    @Query("SELECT c.customerInternalId FROM Customer c WHERE c.customerInternalId LIKE :pattern ORDER BY c.customerInternalId DESC LIMIT 1")
    Optional<String> findLastCustomerInternalId(@Param("pattern") String pattern);
}
