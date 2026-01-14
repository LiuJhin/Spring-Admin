package org.example.cloudopsadmin.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.Payer;
import org.example.cloudopsadmin.service.PayerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.example.cloudopsadmin.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payers")
@RequiredArgsConstructor
public class PayerController {

    private final PayerService payerService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getPayerList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String label,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "asc") String sortOrder
    ) {
        Page<Payer> payerPage = payerService.getPayerList(page, pageSize, search, label, sortBy, sortOrder);

        List<Map<String, Object>> list = payerPage.getContent().stream().map(payer -> {
            Map<String, Object> map = new HashMap<>();
            map.put("payer_internal_id", payer.getPayerInternalId());
            map.put("payer_name", payer.getPayerName());
            map.put("payer_id", payer.getPayerId());
            map.put("contact_email", payer.getContactEmail());
            map.put("signin_url", payer.getSigninUrl());
            map.put("iam_username", payer.getIamUsername());
            map.put("remarks", payer.getRemarks());
            map.put("created_at", payer.getCreatedAt());
            
            List<Map<String, Object>> accounts = payer.getAccounts().stream().map(acc -> {
                Map<String, Object> accMap = new HashMap<>();
                accMap.put("account_id", acc.getAccountInternalId());
                accMap.put("account_name", acc.getAccountName());
                accMap.put("uid", acc.getUid());
                accMap.put("bound_email", acc.getBoundEmail());
                accMap.put("monitor_email", acc.getMonitorEmail());
                accMap.put("labels", acc.getLabels());
                return accMap;
            }).collect(Collectors.toList());
            
            map.put("accounts", accounts);
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("total", payerPage.getTotalElements());
        data.put("page", payerPage.getNumber() + 1);
        data.put("page_size", payerPage.getSize());
        data.put("list", list);

        return ApiResponse.success("success", data);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createPayer(@RequestBody CreatePayerRequest request) {
        try {
            Payer payer = new Payer();
            payer.setPayerName(request.getPayerName());
            payer.setPayerId(request.getPayerId());
            payer.setSigninUrl(request.getSigninUrl());
            payer.setIamUsername(request.getIamUsername());
            payer.setPassword(request.getPassword());
            payer.setContactEmail(request.getContactEmail());
            payer.setRemarks(request.getRemarks());

            Payer savedPayer = payerService.createPayer(payer);

            Map<String, Object> data = new HashMap<>();
            data.put("payer_internal_id", savedPayer.getPayerInternalId());
            data.put("payer_name", savedPayer.getPayerName());
            data.put("payer_id", savedPayer.getPayerId());
            data.put("signin_url", savedPayer.getSigninUrl());
            data.put("iam_username", savedPayer.getIamUsername());
            data.put("contact_email", savedPayer.getContactEmail());
            data.put("remarks", savedPayer.getRemarks());
            data.put("created_at", savedPayer.getCreatedAt());

            return ApiResponse.success("Payer 创建成功", data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @Data
    public static class CreatePayerRequest {
        private String payer_name;
        private String payer_id;
        private String signin_url;
        private String iam_username;
        private String password;
        private String contact_email;
        private String remarks;

        // Getters adapting snake_case JSON to camelCase Java fields
        public String getPayerName() { return payer_name; }
        public String getPayerId() { return payer_id; }
        public String getSigninUrl() { return signin_url; }
        public String getIamUsername() { return iam_username; }
        public String getPassword() { return password; }
        public String getContactEmail() { return contact_email; }
        public String getRemarks() { return remarks; }
    }
}
