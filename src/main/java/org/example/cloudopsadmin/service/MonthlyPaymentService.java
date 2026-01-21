package org.example.cloudopsadmin.service;

import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.entity.MonthlyPayment;
import org.example.cloudopsadmin.entity.Customer;
import org.example.cloudopsadmin.repository.CustomerRepository;
import org.example.cloudopsadmin.repository.CustomerMonthlyBillRepository;
import org.example.cloudopsadmin.repository.MonthlyPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyPaymentService {

    private final CustomerMonthlyBillRepository billRepository;
    private final MonthlyPaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;

    public Map<String, Object> getMonthlyPaymentOverview(String month, String customerNameFilter, String statusFilter) {
        // 1. Get all customers (The Source of Truth)
        List<Customer> customers = customerRepository.findAll();
        Set<String> validCustomerNames = customers.stream()
                .map(Customer::getCustomerName)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());

        // Map for case-insensitive matching: lowercase -> standard name
        Map<String, String> normalizedNameMap = new HashMap<>();
        for (String name : validCustomerNames) {
            normalizedNameMap.put(name.toLowerCase(), name);
        }

        // 2. Get all receivables (bills)
        List<Object[]> billSums = billRepository.sumPayableByMonth(month);
        Map<String, Double> receivableMap = new HashMap<>();
        Map<String, Long> accountCountMap = new HashMap<>();
        for (Object[] row : billSums) {
            String rawName = (String) row[0];
            if (!StringUtils.hasText(rawName)) continue;

            String normalizedKey = rawName.trim().toLowerCase();
            String standardName = normalizedNameMap.get(normalizedKey);

            // Only aggregate if the customer exists in the Customer table
            if (standardName != null) {
                Double amount = (Double) row[1];
                Long count = (Long) row[2];
                receivableMap.merge(standardName, amount != null ? amount : 0.0, Double::sum);
                accountCountMap.merge(standardName, count != null ? count : 0L, Long::sum);
            }
        }

        // 3. Get all payments
        List<MonthlyPayment> payments = paymentRepository.findByMonth(month);
        Map<String, MonthlyPayment> paymentMap = new HashMap<>();
        
        for (MonthlyPayment p : payments) {
            String rawName = p.getCustomerName();
            if (!StringUtils.hasText(rawName)) continue;

            String normalizedKey = rawName.trim().toLowerCase();
            String standardName = normalizedNameMap.get(normalizedKey);

            if (standardName != null) {
                paymentMap.put(standardName, p);
            }
        }
        
        // 4. Merge and Filter (Iterate only over valid customers)
        List<Map<String, Object>> list = new ArrayList<>();
        
        double totalReceivable = 0.0;
        double totalReceived = 0.0;
        double totalOutstanding = 0.0;

        for (String customer : validCustomerNames) {
            // Apply name filter
            if (StringUtils.hasText(customerNameFilter) && !customer.toLowerCase().contains(customerNameFilter.toLowerCase())) {
                continue;
            }

            double receivable = receivableMap.getOrDefault(customer, 0.0);
            long accountCount = accountCountMap.getOrDefault(customer, 0L);
            MonthlyPayment paymentRecord = paymentMap.get(customer);
            double received = paymentRecord != null ? paymentRecord.getReceivedAmount() : 0.0;
            double outstanding = receivable - received;
            if (outstanding < 0) outstanding = 0.0; // Handle overpayment

            String status = calculateStatus(receivable, received, outstanding);

            // Apply status filter
            if (StringUtils.hasText(statusFilter) && !"全部".equals(statusFilter) && !status.equals(statusFilter)) {
                continue;
            }

            // Add to totals
            totalReceivable += receivable;
            totalReceived += received;
            totalOutstanding += outstanding;

            Map<String, Object> item = new HashMap<>();
            item.put("customer_name", customer);
            item.put("account_count", accountCount);
            item.put("receivable_amount", receivable);
            item.put("received_amount", received);
            item.put("outstanding_amount", outstanding);
            item.put("status", status);
            item.put("last_payment_date", paymentRecord != null ? paymentRecord.getLastPaymentDate() : null);
            item.put("payment_method", paymentRecord != null ? paymentRecord.getPaymentMethod() : "-");
            item.put("remarks", paymentRecord != null ? paymentRecord.getRemarks() : "-");
            
            list.add(item);
        }
        
        // Calculate rate
        double rate = totalReceivable > 0 ? (totalReceived / totalReceivable) * 100 : 0.0;
        String rateStr = String.format("%.1f%%", rate);

        Map<String, Object> summary = new HashMap<>();
        summary.put("total_receivable", totalReceivable);
        summary.put("total_received", totalReceived);
        summary.put("outstanding", totalOutstanding);
        summary.put("collection_rate", rateStr);

        Map<String, Object> response = new HashMap<>();
        response.put("summary", summary);
        response.put("list", list);
        
        return response;
    }

    public MonthlyPayment updatePayment(String month, String customerName, Double receivedAmount, String paymentMethod, LocalDate paymentDate, String remarks) {
        MonthlyPayment payment = paymentRepository.findByMonthAndCustomerName(month, customerName)
                .orElse(new MonthlyPayment());
        
        if (payment.getId() == null) {
            payment.setMonth(month);
            payment.setCustomerName(customerName);
        }
        
        if (receivedAmount != null) payment.setReceivedAmount(receivedAmount);
        if (paymentMethod != null) payment.setPaymentMethod(paymentMethod);
        if (paymentDate != null) payment.setLastPaymentDate(paymentDate);
        if (remarks != null) payment.setRemarks(remarks);
        
        return paymentRepository.save(payment);
    }

    private String calculateStatus(double receivable, double received, double outstanding) {
        if (receivable <= 0.01) return "无需付款"; // Or handle as Paid?
        if (outstanding <= 0.01) return "已付款";
        if (received <= 0.01) return "未付款";
        return "部分付款";
    }
}
