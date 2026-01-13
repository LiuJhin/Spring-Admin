package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.CustomerMonthlyBill;
import org.example.cloudopsadmin.service.CustomerMonthlyBillService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping({
        "/api/v1/customer-monthly-bills",
        "/api/v1/customerMonthlyBills",
        "/api/v1/billing/customer/monthly",
        "/api/api/v1/customer-monthly-bills" // Compatibility for frontend path issue
})
@RequiredArgsConstructor
@Tag(name = "Customer Monthly Bill", description = "APIs for customer monthly bills")
public class CustomerMonthlyBillController {

    private final CustomerMonthlyBillService customerMonthlyBillService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerMonthlyBillController.class);

    @GetMapping
    @Operation(summary = "Get customer monthly bill list", description = "List customer monthly bills with filters and sorting")
    public ApiResponse<Map<String, Object>> getCustomerMonthlyBillList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "customer_name", required = false) String customerName,
            @RequestParam(name = "linked_account_uid", required = false) String linkedAccountUid,
            @RequestParam(required = false) String month,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder
    ) {
        try {
            Page<CustomerMonthlyBill> billPage = customerMonthlyBillService.getMonthlyBillList(
                    page,
                    pageSize,
                    customerName,
                    linkedAccountUid,
                    month,
                    sortOrder
            );

            final int[] offsetHolder = { (billPage.getNumber()) * billPage.getSize() };
            List<Map<String, Object>> list = billPage.getContent().stream().map(bill -> {
                Map<String, Object> map = new HashMap<>();
                offsetHolder[0] = offsetHolder[0] + 1;
                map.put("index", offsetHolder[0]);
                map.put("id", bill.getId());
                map.put("cloud_vendor", bill.getCloudVendor());
                map.put("customer_name", bill.getCustomerName());
                map.put("linked_account_uid", bill.getLinkedAccountUid());
                map.put("original_billing_percentage", bill.getOriginalBillingPercentage());
                map.put("total_bill", bill.getTotalBill());
                map.put("undiscounted_bill", bill.getUndiscountedBill());
                map.put("cost_discount_percentage", bill.getCostDiscountPercentage());
                map.put("remarks", bill.getRemarks());
                map.put("month", bill.getMonth());

                Double originalPct = bill.getOriginalBillingPercentage();
                Double undiscounted = bill.getUndiscountedBill();
                Double costPct = bill.getCostDiscountPercentage();

                Double customerPayable = bill.getCustomerPayableBill();
                if (customerPayable == null && originalPct != null && undiscounted != null) {
                    customerPayable = undiscounted * (originalPct / 100.0);
                }

                Double supplierPayable = bill.getSupplierPayableBill();
                if (supplierPayable == null && costPct != null && undiscounted != null) {
                    supplierPayable = undiscounted * (1.0 - (costPct / 100.0));
                }

                Double profit = bill.getProfit();
                if (profit == null && customerPayable != null && supplierPayable != null) {
                    profit = customerPayable - supplierPayable;
                }

                map.put("customer_payable_bill", customerPayable);
                map.put("supplier_payable_bill", supplierPayable);
                map.put("profit", profit);

                final Double[] lastMonthUsage = { null };
                final Double[] lastMonthProfit = { null };
                final Double[] usageMoM = { null };
                final Double[] profitMoM = { null };
                customerMonthlyBillService.findPreviousMonthBill(bill.getMonth(), bill.getLinkedAccountUid()).ifPresent(prev -> {
                    Double prevUndiscounted = prev.getUndiscountedBill();
                    
                    Double prevProfit = prev.getProfit();
                    if (prevProfit == null) {
                        Double prevOriginalPct = prev.getOriginalBillingPercentage();
                        Double prevCostPct = prev.getCostDiscountPercentage();
                        Double prevCustomerPayable = (prevUndiscounted != null && prevOriginalPct != null) ? prevUndiscounted * (prevOriginalPct / 100.0) : null;
                        Double prevSupplierPayable = (prevUndiscounted != null && prevCostPct != null) ? prevUndiscounted * (1.0 - (prevCostPct / 100.0)) : null;
                        prevProfit = (prevCustomerPayable != null && prevSupplierPayable != null) ? (prevCustomerPayable - prevSupplierPayable) : null;
                    }

                    lastMonthUsage[0] = prevUndiscounted;
                    lastMonthProfit[0] = prevProfit;
                });

                map.put("last_month_usage", lastMonthUsage[0]);
                map.put("last_month_profit", lastMonthProfit[0]);

                if (lastMonthUsage[0] != null && undiscounted != null && lastMonthUsage[0] != 0.0) {
                    usageMoM[0] = (undiscounted - lastMonthUsage[0]) / lastMonthUsage[0];
                }
                if (lastMonthProfit[0] != null && profit != null && lastMonthProfit[0] != 0.0) {
                    profitMoM[0] = (profit - lastMonthProfit[0]) / lastMonthProfit[0];
                }
                map.put("usage_mom", usageMoM[0]);
                map.put("profit_mom", profitMoM[0]);
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("total", billPage.getTotalElements());
            data.put("page", billPage.getNumber() + 1);
            data.put("page_size", billPage.getSize());
            data.put("list", list);

            return ApiResponse.success("success", data);
        } catch (Exception e) {
            log.error("Get customer monthly bills failed. page={}, pageSize={}, customerName={}, linkedAccountUid={}, month={}, sortOrder={}",
                    page, pageSize, customerName, linkedAccountUid, month, sortOrder, e);
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer monthly bill", description = "Update total bill, undiscounted bill and financial details")
    public ApiResponse<CustomerMonthlyBill> updateBill(
            @PathVariable Long id,
            @RequestBody UpdateBillRequest request
    ) {
        try {
            CustomerMonthlyBill updatedBill = customerMonthlyBillService.updateBill(
                    id,
                    request.getTotal_bill(),
                    request.getUndiscounted_bill(),
                    request.getCustomer_payable_bill(),
                    request.getSupplier_payable_bill(),
                    request.getProfit()
            );
            return ApiResponse.success("success", updatedBill);
        } catch (Exception e) {
            log.error("Update customer monthly bill failed. id={}, request={}", id, request, e);
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }

    @Data
    public static class UpdateBillRequest {
        private Double total_bill;
        private Double undiscounted_bill;
        private Double customer_payable_bill;
        private Double supplier_payable_bill;
        private Double profit;
    }
}
