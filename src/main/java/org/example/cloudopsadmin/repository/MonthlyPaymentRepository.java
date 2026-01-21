package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.MonthlyPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface MonthlyPaymentRepository extends JpaRepository<MonthlyPayment, Long> {
    Optional<MonthlyPayment> findByMonthAndCustomerName(String month, String customerName);
    List<MonthlyPayment> findByMonth(String month);
}
