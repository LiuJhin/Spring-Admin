package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.CustomerMonthlyBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerMonthlyBillRepository extends JpaRepository<CustomerMonthlyBill, Long>, JpaSpecificationExecutor<CustomerMonthlyBill> {
}

