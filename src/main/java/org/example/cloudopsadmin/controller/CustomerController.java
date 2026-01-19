package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.Customer;
import org.example.cloudopsadmin.entity.Account;
import org.example.cloudopsadmin.entity.User;
import org.example.cloudopsadmin.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Collections;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "APIs for managing customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "Get customer list", description = "Get list of customers with pagination, search, and filtering")
    public ApiResponse<Map<String, Object>> getCustomerList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String label,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder
    ) {
        Page<Customer> customerPage = customerService.getCustomerList(page, pageSize, search, status, label, sortBy, sortOrder);

        List<Map<String, Object>> list = customerPage.getContent().stream().map(customer -> {
            Map<String, Object> map = new HashMap<>();
            map.put("customer_internal_id", customer.getCustomerInternalId());
            map.put("customer_name", customer.getCustomerName());
            map.put("email", customer.getEmail());
            map.put("status", customer.getStatus());
            map.put("company", customer.getCompany());
            map.put("delivery_time", customer.getDeliveryTime());
            map.put("labels", customer.getLabels());
            map.put("remarks", customer.getRemarks());
            map.put("created_at", customer.getCreatedAt());
            map.put("products_used", customer.getProductsUsed());
            map.put("permissions", customer.getPermissions());
            map.put("original_billing_percentage", customer.getOriginalBillingPercentage());
            
            // Payer Info (prefer Customer.payer, fallback to first Account.payer)
            org.example.cloudopsadmin.entity.Payer payerEntity = customer.getPayer();
            if (payerEntity == null && customer.getAccounts() != null) {
                for (Account account : customer.getAccounts()) {
                    if (account != null && account.getPayer() != null) {
                        payerEntity = account.getPayer();
                        break;
                    }
                }
            }
            if (payerEntity != null) {
                Map<String, Object> payer = new HashMap<>();
                payer.put("payer_id", payerEntity.getPayerInternalId());
                payer.put("payer_internal_id", payerEntity.getPayerInternalId());
                payer.put("payer_name", payerEntity.getPayerName());
                map.put("payer", payer);
            }
            
            // UIDs Info
            List<Map<String, Object>> uids = new java.util.ArrayList<>();
            
            // Add manual UIDs
            if (customer.getUids() != null) {
                uids.addAll(customer.getUids().stream().map(uid -> {
                    Map<String, Object> uidMap = new HashMap<>();
                    uidMap.put("uid", uid.getUid());
                    uidMap.put("uid_type", uid.getUidType());
                    uidMap.put("is_primary", uid.getIsPrimary());
                    uidMap.put("description", uid.getDescription());
                    return uidMap;
                }).collect(Collectors.toList()));
            }
            
            // Add Account UIDs
            if (customer.getAccounts() != null) {
                uids.addAll(customer.getAccounts().stream().map(account -> {
                    Map<String, Object> uidMap = new HashMap<>();
                    uidMap.put("uid", account.getUid());
                    uidMap.put("uid_type", account.getAccountType());
                    uidMap.put("is_primary", false);
                    uidMap.put("description", account.getAccountName());
                    return uidMap;
                }).collect(Collectors.toList()));
            }
            
            map.put("uids", uids);

            return map;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("total", customerPage.getTotalElements());
        data.put("page", customerPage.getNumber() + 1);
        data.put("page_size", customerPage.getSize());
        data.put("list", list);

        return ApiResponse.success("success", data);
    }

    @PostMapping
    @Operation(summary = "Create a new customer", description = "Creates a new customer with multiple UIDs.")
    public ApiResponse<Map<String, Object>> createCustomer(@RequestBody CustomerService.CreateCustomerRequest request) {
        try {
            User operator = (User) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            Customer savedCustomer = customerService.createCustomer(request, operator);

            Map<String, Object> data = new HashMap<>();
            data.put("customer_internal_id", savedCustomer.getCustomerInternalId());
            data.put("customer_name", savedCustomer.getCustomerName());
            data.put("email", savedCustomer.getEmail());
            data.put("status", savedCustomer.getStatus());
            data.put("created_at", savedCustomer.getCreatedAt());

            if (savedCustomer.getPayer() != null) {
                data.put("payer_id", savedCustomer.getPayer().getPayerInternalId()); // Return internal ID or business ID as preferred
            }

            List<Map<String, Object>> uids = (savedCustomer.getUids() == null ? Collections.<org.example.cloudopsadmin.entity.CustomerUid>emptyList() : savedCustomer.getUids())
                    .stream().map(uid -> {
                Map<String, Object> uidMap = new HashMap<>();
                uidMap.put("uid", uid.getUid());
                uidMap.put("uid_type", uid.getUidType());
                uidMap.put("is_primary", uid.getIsPrimary());
                uidMap.put("description", uid.getDescription());
                return uidMap;
            }).collect(Collectors.toList());

            data.put("uids", uids);

            return ApiResponse.success("Customer created successfully", data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Internal Server Error: " + e.getMessage());
        }
    }
}
