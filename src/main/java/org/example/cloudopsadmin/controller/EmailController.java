package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.Email;
import org.example.cloudopsadmin.service.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
@Tag(name = "Email Management", description = "APIs for managing emails")
public class EmailController {

    private final EmailService emailService;

    @GetMapping
    @Operation(summary = "Get email list", description = "Get list of emails with pagination, search, and filtering")
    public ApiResponse<Map<String, Object>> getEmailList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String label,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder
    ) {
        Page<Email> emailPage = emailService.getEmailList(page, pageSize, search, category, status, label, sortBy, sortOrder);

        List<Map<String, Object>> list = emailPage.getContent().stream().map(email -> {
            Map<String, Object> map = new HashMap<>();
            map.put("email_internal_id", email.getEmailInternalId());
            map.put("email_address", email.getEmailAddress());
            map.put("source", email.getSource());
            map.put("category", email.getCategory());
            map.put("status", email.getStatus());
            map.put("labels", email.getLabels());
            map.put("remarks", email.getRemarks());
            map.put("created_at", email.getCreatedAt() != null ? email.getCreatedAt().toString() : null);
            
            // Parent Email Info
            if (email.getParentEmail() != null) {
                Map<String, Object> parent = new HashMap<>();
                parent.put("email_internal_id", email.getParentEmail().getEmailInternalId());
                parent.put("email_address", email.getParentEmail().getEmailAddress());
                map.put("parent_email", parent);
            }

            // Payer Info
            if (email.getPayer() != null) {
                 Map<String, Object> payer = new HashMap<>();
                 payer.put("payer_id", email.getPayer().getPayerId());
                 payer.put("payer_name", email.getPayer().getPayerName());
                 map.put("payer", payer);
            }
            
            // Additional info
            map.put("credit_card_last4", email.getCreditCardLast4());
            map.put("is_sp_account", email.getIsSpAccount());
            map.put("linked_account_name", email.getLinkedAccountName());
            map.put("linked_account_uid", email.getLinkedAccountUid());
            map.put("enable_forwarding", email.getEnableForwarding());

            return map;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("total", emailPage.getTotalElements());
        data.put("page", emailPage.getNumber() + 1);
        data.put("page_size", emailPage.getSize());
        data.put("list", list);

        return ApiResponse.success("success", data);
    }

    @PostMapping
    @Operation(summary = "Create a new email", description = "Creates a new email entry. Supports primary and secondary emails.")
    public ApiResponse<Map<String, Object>> createEmail(@RequestBody EmailService.CreateEmailRequest request) {
        try {
            Email savedEmail = emailService.createEmail(request);

            Map<String, Object> data = new HashMap<>();
            data.put("email_internal_id", savedEmail.getEmailInternalId());
            data.put("email_address", savedEmail.getEmailAddress());
            data.put("source", savedEmail.getSource());
            data.put("category", savedEmail.getCategory());
            data.put("status", savedEmail.getStatus());
            data.put("created_at", savedEmail.getCreatedAt() != null ? savedEmail.getCreatedAt().toString() : null);
            
            if (savedEmail.getParentEmail() != null) {
                data.put("parent_email_id", savedEmail.getParentEmail().getEmailInternalId());
            }
            
            if (savedEmail.getPayer() != null) {
                data.put("payer_id", savedEmail.getPayer().getPayerId());
            }

            return ApiResponse.success("Email created successfully", data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Internal Server Error: " + e.getMessage());
        }
    }
}
