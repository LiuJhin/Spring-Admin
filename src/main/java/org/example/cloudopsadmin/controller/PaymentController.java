package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.MonthlyPayment;
import org.example.cloudopsadmin.service.MonthlyPaymentService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "Finance Management")
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class PaymentController {

    private final MonthlyPaymentService monthlyPaymentService;

    @Operation(summary = "Get monthly payment overview and list")
    @GetMapping("/monthly-payments")
    public ApiResponse<Map<String, Object>> getMonthlyPayments(
            @RequestParam String month,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success("success", monthlyPaymentService.getMonthlyPaymentOverview(month, customerName, status));
    }

    @Operation(summary = "Update payment record")
    @PostMapping("/monthly-payments/record")
    public ApiResponse<MonthlyPayment> recordPayment(@RequestBody RecordPaymentRequest request) {
        MonthlyPayment payment = monthlyPaymentService.updatePayment(
                request.getMonth(),
                request.getCustomerName(),
                request.getReceivedAmount(),
                request.getPaymentMethod(),
                request.getLastPaymentDate(),
                request.getRemarks()
        );
        return ApiResponse.success("success", payment);
    }

    @Data
    public static class RecordPaymentRequest {
        private String month;
        private String customerName;
        private Double receivedAmount;
        private String paymentMethod;
        private LocalDate lastPaymentDate;
        private String remarks;
    }
}
