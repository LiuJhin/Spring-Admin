package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.Account;
import org.example.cloudopsadmin.entity.User;
import org.example.cloudopsadmin.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "APIs for managing accounts")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "Get account list", description = "Get list of accounts with pagination, search, and filtering")
    public ApiResponse<Map<String, Object>> getAccountList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "account_type", required = false) String accountType,
            @RequestParam(name = "account_source", required = false) String accountSource,
            @RequestParam(required = false) String label,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder
    ) {
        Page<Account> accountPage = accountService.getAccountList(page, pageSize, search, accountType, accountSource, label, sortBy, sortOrder);

        List<Map<String, Object>> list = accountPage.getContent().stream().map(account -> {
            Map<String, Object> map = new HashMap<>();
            map.put("account_internal_id", account.getAccountInternalId());
            map.put("uid", account.getUid());
            map.put("account_name", account.getAccountName());
            map.put("account_type", account.getAccountType());
            map.put("account_category", account.getAccountCategory());
            map.put("account_source", account.getAccountSource());
            map.put("is_submitted", account.getIsSubmitted());
            map.put("created_at", account.getCreatedAt());
            map.put("labels", account.getLabels());

            if (account.getPayer() != null) {
                Map<String, Object> payerMap = new HashMap<>();
                payerMap.put("payer_id", account.getPayer().getPayerInternalId());
                payerMap.put("payer_name", account.getPayer().getPayerName());
                map.put("payer", payerMap);
            }

            if (account.getCustomer() != null) {
                Map<String, Object> customerMap = new HashMap<>();
                customerMap.put("customer_id", account.getCustomer().getCustomerInternalId());
                customerMap.put("customer_name", account.getCustomer().getCustomerName());
                map.put("customer", customerMap);
            }

            return map;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("total", accountPage.getTotalElements());
        data.put("page", accountPage.getNumber() + 1);
        data.put("page_size", accountPage.getSize());
        data.put("list", list);

        return ApiResponse.success("success", data);
    }

    @GetMapping("/sp-list")
    @Operation(summary = "Get SP account list", description = "Get list of SP accounts with pagination, search, and filtering")
    public ApiResponse<Map<String, Object>> getSpAccountList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "account_type", required = false) String accountType,
            @RequestParam(name = "account_source", required = false) String accountSource,
            @RequestParam(required = false) String label,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder
    ) {
        Page<Account> accountPage = accountService.getSpAccountList(page, pageSize, search, accountType, accountSource, label, sortBy, sortOrder);

        List<Map<String, Object>> list = accountPage.getContent().stream().map(account -> {
            Map<String, Object> map = new HashMap<>();
            map.put("account_internal_id", account.getAccountInternalId());
            map.put("uid", account.getUid());
            map.put("account_name", account.getAccountName());
            map.put("account_type", account.getAccountType());
            map.put("account_category", account.getAccountCategory());
            map.put("account_source", account.getAccountSource());
            map.put("is_submitted", account.getIsSubmitted());
            map.put("is_monitored_sp", account.getIsMonitoredSp());
            map.put("created_at", account.getCreatedAt());
            map.put("labels", account.getLabels());

            if (account.getPayer() != null) {
                Map<String, Object> payerMap = new HashMap<>();
                payerMap.put("payer_id", account.getPayer().getPayerInternalId());
                payerMap.put("payer_name", account.getPayer().getPayerName());
                map.put("payer", payerMap);
            }

            if (account.getCustomer() != null) {
                Map<String, Object> customerMap = new HashMap<>();
                customerMap.put("customer_id", account.getCustomer().getCustomerInternalId());
                customerMap.put("customer_name", account.getCustomer().getCustomerName());
                map.put("customer", customerMap);
            }

            return map;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("total", accountPage.getTotalElements());
        data.put("page", accountPage.getNumber() + 1);
        data.put("page_size", accountPage.getSize());
        data.put("list", list);

        return ApiResponse.success("success", data);
    }

    @GetMapping("/detail")
    @Operation(summary = "Get account detail", description = "Get account detail by account_id or uid")
    public ApiResponse<Map<String, Object>> getAccountDetail(
            @RequestParam(name = "account_id", required = false) String accountId,
            @RequestParam(required = false) String uid
    ) {
        try {
            Map<String, Object> data = accountService.getAccountDetail(accountId, uid);
            return ApiResponse.success("success", data);
        } catch (AccountService.ApiException e) {
            return ApiResponse.error(e.getCode(), e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }

    @PostMapping("/addAccount")
    @Operation(summary = "Add account", description = "RPC style endpoint to add an account with associations")
    public ApiResponse<Map<String, Object>> addAccount(@RequestBody AccountService.AddAccountRequest request) {
        try {
            User operator = (User) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            Map<String, Object> data = accountService.addAccount(request, operator);
            return ApiResponse.success("账号新增成功", data);
        } catch (AccountService.ApiException e) {
            return ApiResponse.error(e.getCode(), e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }
}
