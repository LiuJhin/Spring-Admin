package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.Invoice;
import org.example.cloudopsadmin.entity.InvoiceLineItem;
import org.example.cloudopsadmin.entity.User;
import org.example.cloudopsadmin.service.InvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Create and manage invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InvoiceController.class);

    @GetMapping
    @Operation(summary = "获取发票列表", description = "分页获取发票列表，支持搜索和状态过滤")
    public ApiResponse<Map<String, Object>> getInvoiceList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder
    ) {
        Page<Invoice> invoicePage = invoiceService.getInvoiceList(page, pageSize, search, status, sortBy, sortOrder);

        List<Map<String, Object>> list = invoicePage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("total", invoicePage.getTotalElements());
        data.put("page", invoicePage.getNumber() + 1);
        data.put("page_size", invoicePage.getSize());
        data.put("list", list);

        return ApiResponse.success("success", data);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取发票详情", description = "根据ID获取发票详情")
    public ApiResponse<Map<String, Object>> getInvoice(@PathVariable Long id) {
        try {
            Invoice invoice = invoiceService.getInvoice(id);
            return ApiResponse.success("success", toResponse(invoice));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "创建发票", description = "创建发票及其行项目，并返回合计信息")
    public ApiResponse<Map<String, Object>> createInvoice(@RequestBody InvoiceService.CreateInvoiceRequest request) {
        try {
            User operator = (User) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            Invoice saved = invoiceService.createInvoice(request, operator);
            Map<String, Object> data = toResponse(saved);
            return ApiResponse.success("Invoice created successfully", data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Create invoice failed", e);
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }

    @DeleteMapping
    @Operation(summary = "删除发票", description = "支持按ID列表批量删除发票，并清理关联账单状态")
    public ApiResponse<Object> deleteInvoices(@RequestBody Map<String, List<Long>> body) {
        try {
            List<Long> ids = body != null ? body.get("ids") : null;
            if (ids == null || ids.isEmpty()) {
                return ApiResponse.error(400, "ids不能为空");
            }
            User operator = (User) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            invoiceService.deleteInvoices(Set.copyOf(ids), operator);
            return ApiResponse.success("success", null);
        } catch (Exception e) {
            log.error("Delete invoices failed. body={}", body, e);
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    @Operation(summary = "更新发票草稿", description = "部分更新发票草稿字段及行项目")
    public ApiResponse<Map<String, Object>> patchInvoice(@PathVariable Long id,
                                                         @RequestBody InvoiceService.UpdateInvoiceRequest request) {
        try {
            User operator = (User) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            Invoice updated = invoiceService.updateInvoice(id, request, operator);
            return ApiResponse.success("success", toResponse(updated));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("Patch invoice failed", e);
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新发票", description = "全量更新发票字段及行项目")
    public ApiResponse<Map<String, Object>> putInvoice(@PathVariable Long id,
                                                       @RequestBody InvoiceService.UpdateInvoiceRequest request) {
        try {
            User operator = (User) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            Invoice updated = invoiceService.updateInvoice(id, request, operator);
            return ApiResponse.success("success", toResponse(updated));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("Put invoice failed", e);
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "过账发票", description = "将发票从草稿状态变为已过账，并生成付款参考号")
    public ApiResponse<Map<String, Object>> postInvoice(@PathVariable Long id) {
        try {
            User operator = (User) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            Invoice posted = invoiceService.postInvoice(id, operator);
            return ApiResponse.success("success", toResponse(posted));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("Post invoice failed", e);
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ApiResponse<Object> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException e) {
        log.error("JSON parse error", e);
        return ApiResponse.error(400, "Invalid JSON format: " + e.getMessage());
    }

    private Map<String, Object> toResponse(Invoice invoice) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", invoice.getId());
        map.put("customer_name", invoice.getCustomerName());
        map.put("invoice_date", invoice.getInvoiceDate());
        map.put("due_date", invoice.getDueDate());
        map.put("currency", invoice.getCurrency());
        map.put("tax_number", invoice.getTaxNumber());
        map.put("payment_reference", invoice.getPaymentReference());
        map.put("activities", invoice.getActivities());
        map.put("terms", invoice.getTerms());
        map.put("status", invoice.getStatus());

        List<Map<String, Object>> lineItems = invoice.getItems().stream().map(this::toItemMap).collect(Collectors.toList());
        map.put("items", lineItems);

        map.put("subtotal_ex_tax", invoice.getSubtotalExTax());
        map.put("tax_total", invoice.getTaxTotal());
        map.put("grand_total", invoice.getGrandTotal());
        map.put("created_at", invoice.getCreatedAt());
        return map;
    }

    private Map<String, Object> toItemMap(InvoiceLineItem item) {
        Map<String, Object> m = new HashMap<>();
        m.put("product_id", item.getProductId());
        m.put("label", item.getLabel());
        m.put("quantity", item.getQuantity());
        m.put("price", item.getPrice());
        m.put("discount_pct", item.getDiscountPct());
        m.put("tax_pct", item.getTaxPct());
        m.put("amount_ex_tax", item.getAmountExTax());
        m.put("amount_inc_tax", item.getAmountIncTax());
        return m;
    }

}
