package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.CustomerMonthlyBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CustomerMonthlyBillRepository extends JpaRepository<CustomerMonthlyBill, Long>, JpaSpecificationExecutor<CustomerMonthlyBill> {
    
    @Query("SELECT c.customerName, SUM(b.customerPayableBill), COUNT(DISTINCT b.linkedAccountUid) FROM CustomerMonthlyBill b LEFT JOIN b.customer c WHERE b.month = :month GROUP BY c.customerName")
    List<Object[]> sumPayableByMonth(@Param("month") String month);
}

