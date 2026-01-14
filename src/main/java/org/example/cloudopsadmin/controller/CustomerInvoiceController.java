package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.CustomerMonthlyBill;
import org.example.cloudopsadmin.service.CustomerMonthlyBillService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customer-invoices")
@RequiredArgsConstructor
@Tag(name = "Customer Invoices", description = "Invoices list grouped by customer per month")
public class CustomerInvoiceController {

    private final CustomerMonthlyBillService customerMonthlyBillService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerInvoiceController.class);

    @GetMapping
    @Operation(summary = "List customer invoices", description = "Group monthly bills by customer with filters and pagination")
    public ApiResponse<Map<String, Object>> listCustomerInvoices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "customer_name", required = false) String customerName,
            @RequestParam(name = "linked_account_uid", required = false) String linkedAccountUid,
            @RequestParam(name = "cloud_vendor", required = false) String cloudVendor,
            @RequestParam(required = false) String month,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder
    ) {
        try {
            List<CustomerMonthlyBill> bills = customerMonthlyBillService.listBillsByFilters(
                    month,
                    customerName,
                    linkedAccountUid,
                    cloudVendor
            );

            Map<String, List<CustomerMonthlyBill>> grouped = bills.stream()
                    .collect(Collectors.groupingBy(CustomerMonthlyBill::getCustomerName, LinkedHashMap::new, Collectors.toList()));

            List<Map<String, Object>> aggregated = new ArrayList<>();
            for (Map.Entry<String, List<CustomerMonthlyBill>> entry : grouped.entrySet()) {
                String custName = entry.getKey();
                List<CustomerMonthlyBill> custBills = entry.getValue();
                String m = custBills.isEmpty() ? month : custBills.get(0).getMonth();

                double totalSum = custBills.stream().map(CustomerMonthlyBill::getTotalBill).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
                double undiscountedSum = custBills.stream().map(CustomerMonthlyBill::getUndiscountedBill).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();

                double customerPayableSum = custBills.stream().map(b -> {
                    Double v = b.getCustomerPayableBill();
                    if (v == null) {
                        Double pct = b.getOriginalBillingPercentage();
                        Double und = b.getUndiscountedBill();
                        v = (pct != null && und != null) ? und * (pct / 100.0) : null;
                    }
                    return v;
                }).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();

                double supplierPayableSum = custBills.stream().map(b -> {
                    Double v = b.getSupplierPayableBill();
                    if (v == null) {
                        Double pct = b.getCostDiscountPercentage();
                        Double und = b.getUndiscountedBill();
                        v = (pct != null && und != null) ? und * (1.0 - (pct / 100.0)) : null;
                    }
                    return v;
                }).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();

                double profitSum = custBills.stream().map(b -> {
                    Double v = b.getProfit();
                    if (v == null) {
                        Double cp = b.getCustomerPayableBill();
                        if (cp == null) {
                            Double pct = b.getOriginalBillingPercentage();
                            Double und = b.getUndiscountedBill();
                            cp = (pct != null && und != null) ? und * (pct / 100.0) : null;
                        }
                        Double sp = b.getSupplierPayableBill();
                        if (sp == null) {
                            Double pct = b.getCostDiscountPercentage();
                            Double und = b.getUndiscountedBill();
                            sp = (pct != null && und != null) ? und * (1.0 - (pct / 100.0)) : null;
                        }
                        v = (cp != null && sp != null) ? (cp - sp) : null;
                    }
                    return v;
                }).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();

                Set<String> vendors = custBills.stream().map(CustomerMonthlyBill::getCloudVendor).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));

                List<Map<String, Object>> accounts = custBills.stream().map(b -> {
                    Map<String, Object> am = new HashMap<>();
                    am.put("id", b.getId());
                    am.put("uid", b.getLinkedAccountUid());
                    am.put("account_name", b.getAccount() != null ? b.getAccount().getAccountName() : null);
                    am.put("cloud_vendor", b.getCloudVendor());
                    am.put("total_bill", b.getTotalBill());
                    am.put("undiscounted_bill", b.getUndiscountedBill());
                    Double cp = b.getCustomerPayableBill();
                    if (cp == null) {
                        Double pct = b.getOriginalBillingPercentage();
                        Double und = b.getUndiscountedBill();
                        cp = (pct != null && und != null) ? und * (pct / 100.0) : null;
                    }
                    Double sp = b.getSupplierPayableBill();
                    if (sp == null) {
                        Double pct = b.getCostDiscountPercentage();
                        Double und = b.getUndiscountedBill();
                        sp = (pct != null && und != null) ? und * (1.0 - (pct / 100.0)) : null;
                    }
                    Double pf = b.getProfit();
                    if (pf == null && cp != null && sp != null) {
                        pf = cp - sp;
                    }
                    am.put("customer_payable_bill", cp);
                    am.put("supplier_payable_bill", sp);
                    am.put("profit", pf);
                    return am;
                }).collect(Collectors.toList());

                Map<String, Object> mobj = new LinkedHashMap<>();
                mobj.put("customer_name", custName);
                mobj.put("month", m);
                Long billId = custBills.isEmpty() ? null : custBills.get(0).getId();
                mobj.put("customer_monthly_bill_id", billId);
                mobj.put("cloud_vendors", vendors);
                mobj.put("total_bill_sum", totalSum);
                mobj.put("undiscounted_bill_sum", undiscountedSum);
                mobj.put("customer_payable_sum", customerPayableSum);
                mobj.put("supplier_payable_sum", supplierPayableSum);
                mobj.put("profit_sum", profitSum);
                mobj.put("accounts", accounts);

                boolean anyInvoiced = custBills.stream().anyMatch(b -> Boolean.TRUE.equals(b.getIsInvoiced()));
                List<org.example.cloudopsadmin.common.InvoiceStatus> statuses = custBills.stream()
                        .map(CustomerMonthlyBill::getInvoiceStatus)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                org.example.cloudopsadmin.common.InvoiceStatus aggStatus;
                if (statuses.stream().anyMatch(s -> s == org.example.cloudopsadmin.common.InvoiceStatus.OVERDUE)) {
                    aggStatus = org.example.cloudopsadmin.common.InvoiceStatus.OVERDUE;
                } else if (!statuses.isEmpty() && statuses.stream().allMatch(s -> s == org.example.cloudopsadmin.common.InvoiceStatus.PAID)) {
                    aggStatus = org.example.cloudopsadmin.common.InvoiceStatus.PAID;
                } else if (statuses.stream().anyMatch(s -> s == org.example.cloudopsadmin.common.InvoiceStatus.SENT)) {
                    aggStatus = org.example.cloudopsadmin.common.InvoiceStatus.SENT;
                } else if (statuses.stream().anyMatch(s -> s == org.example.cloudopsadmin.common.InvoiceStatus.POSTED)) {
                    aggStatus = org.example.cloudopsadmin.common.InvoiceStatus.POSTED;
                } else {
                    aggStatus = org.example.cloudopsadmin.common.InvoiceStatus.DRAFT;
                }
                boolean aggInvoiced = anyInvoiced || aggStatus != org.example.cloudopsadmin.common.InvoiceStatus.DRAFT;
                mobj.put("is_invoiced", aggInvoiced);
                mobj.put("invoice_status", aggStatus);
                aggregated.add(mobj);
            }

            Comparator<Map<String, Object>> cmp = Comparator.comparingDouble(m -> ((Number) m.getOrDefault("total_bill_sum", 0.0)).doubleValue());
            if ("desc".equalsIgnoreCase(sortOrder)) {
                cmp = cmp.reversed();
            }
            aggregated.sort(cmp);

            int total = aggregated.size();
            int from = Math.max(0, (page - 1) * pageSize);
            int to = Math.min(total, from + pageSize);
            List<Map<String, Object>> pageList = from < to ? aggregated.subList(from, to) : Collections.emptyList();

            Map<String, Object> data = new HashMap<>();
            data.put("total", total);
            data.put("page", page);
            data.put("page_size", pageSize);
            data.put("list", pageList);
            return ApiResponse.success("success", data);
        } catch (Exception e) {
            log.error("List customer invoices failed. page={}, pageSize={}, customerName={}, linkedAccountUid={}, cloudVendor={}, month={}, sortOrder={}",
                    page, pageSize, customerName, linkedAccountUid, cloudVendor, month, sortOrder, e);
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }
}
