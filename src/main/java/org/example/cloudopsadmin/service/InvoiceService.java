package org.example.cloudopsadmin.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.InvoiceStatus;
import org.example.cloudopsadmin.entity.Invoice;
import org.example.cloudopsadmin.entity.InvoiceLineItem;
import org.example.cloudopsadmin.entity.CustomerMonthlyBill;
import org.example.cloudopsadmin.entity.User;
import org.example.cloudopsadmin.repository.CustomerMonthlyBillRepository;
import org.example.cloudopsadmin.repository.InvoiceRepository;
import org.example.cloudopsadmin.service.OperationLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerMonthlyBillRepository customerMonthlyBillRepository;
    private final OperationLogService operationLogService;

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoiceList(int page, int pageSize, String search, String status, String sortBy, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder == null ? "DESC" : sortOrder),
                sortBy == null || sortBy.isEmpty() ? "id" : sortBy);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String likePattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("customerName")), likePattern));
            }

            if (StringUtils.hasText(status)) {
                try {
                    predicates.add(cb.equal(root.get("status"), InvoiceStatus.valueOf(status)));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid status
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return invoiceRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoice(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    }

    @Transactional
    public void deleteInvoices(Set<Long> ids, User operator) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<CustomerMonthlyBill> relatedBills = customerMonthlyBillRepository.findAll((root, query, cb) ->
                root.get("invoiceId").in(ids)
        );
        for (CustomerMonthlyBill bill : relatedBills) {
            bill.setInvoiceId(null);
            bill.setIsInvoiced(false);
            bill.setInvoiceStatus(InvoiceStatus.DRAFT);
            customerMonthlyBillRepository.save(bill);
        }
        customerMonthlyBillRepository.flush();
        if (ids != null && !ids.isEmpty()) {
            if (operator != null) {
                for (Long id : ids) {
                    operationLogService.log(
                            operator.getEmail(),
                            operator.getName(),
                            "DELETE",
                            "invoice",
                            String.valueOf(id),
                            "删除发票: " + id
                    );
                }
            }
            invoiceRepository.deleteAllById(ids);
        }
    }

    @Transactional
    public Invoice createInvoice(CreateInvoiceRequest request, User operator) {
        // 1. Validate and fetch the target bill
        if (request.getCustomerMonthlyBillId() == null) {
            // Fallback to simple create if no bill ID provided (legacy behavior)
             return createSimpleInvoice(request, operator);
        }

        CustomerMonthlyBill targetBill = customerMonthlyBillRepository.findById(request.getCustomerMonthlyBillId())
                .orElseThrow(() -> new IllegalArgumentException("Customer Monthly Bill not found: " + request.getCustomerMonthlyBillId()));

        String customerName = targetBill.getCustomerName();
        String month = targetBill.getMonth();

        // 2. Find all bills for this customer and month
        List<CustomerMonthlyBill> monthlyBills = customerMonthlyBillRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("customerName"), customerName),
                cb.equal(root.get("month"), month)
        ));

        // 3. Check if an invoice already exists for any of these bills
        Long existingInvoiceId = monthlyBills.stream()
                .map(CustomerMonthlyBill::getInvoiceId)
                .filter(transactionId -> transactionId != null)
                .findFirst()
                .orElse(null);

        Invoice invoice;
        boolean isNew = false;

        if (existingInvoiceId != null) {
            invoice = invoiceRepository.findById(existingInvoiceId)
                    .orElseThrow(() -> new IllegalStateException("Invoice found in bill but not in DB: " + existingInvoiceId));
        } else {
            isNew = true;
            invoice = new Invoice();
            invoice.setCustomerName(customerName);
            LocalDate invDate = request.getInvoiceDate() != null ? request.getInvoiceDate() : LocalDate.now();
            invoice.setInvoiceDate(invDate);
            LocalDate due = request.getDueDate() != null ? request.getDueDate() : invDate.plusDays(30);
            invoice.setDueDate(due);
            String currency = request.getCurrency() != null ? request.getCurrency() : "CNY";
            invoice.setCurrency(currency);
            invoice.setTaxNumber(request.getTaxNumber());
            invoice.setPaymentReference(request.getPaymentReference());
            invoice.setActivities(request.getActivities() != null ? request.getActivities() : new ArrayList<>());
            invoice.setTerms(request.getTerms());
            invoice.setStatus(InvoiceStatus.DRAFT);
            // Save first to get ID
            invoice = invoiceRepository.save(invoice);
        }

        // 4. Link all unlinked bills to this invoice
        boolean targetBillProcessed = false;
        for (CustomerMonthlyBill bill : monthlyBills) {
            if (bill.getId().equals(targetBill.getId())) {
                targetBillProcessed = true;
            }
            if (bill.getInvoiceId() == null) {
                bill.setInvoiceId(invoice.getId());
                bill.setIsInvoiced(true);
                bill.setInvoiceStatus(InvoiceStatus.DRAFT);
                customerMonthlyBillRepository.save(bill);
            }
        }

        // Ensure target bill is updated if it wasn't in the list
        if (!targetBillProcessed) {
             if (targetBill.getInvoiceId() == null) {
                 targetBill.setInvoiceId(invoice.getId());
                 targetBill.setIsInvoiced(true);
                 targetBill.setInvoiceStatus(InvoiceStatus.DRAFT);
                 customerMonthlyBillRepository.save(targetBill);
             }
        }
        
        customerMonthlyBillRepository.flush();

        // 5. Add line items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<InvoiceLineItem> newItems = new ArrayList<>();
            for (CreateInvoiceRequest.LineItem li : request.getItems()) {
                InvoiceLineItem item = new InvoiceLineItem();
                item.setInvoice(invoice);
                item.setProductId(li.getProductId());
                item.setLabel(li.getLabel());
                item.setQuantity(li.getQuantity() == null ? 1 : Math.max(1, li.getQuantity()));
                item.setPrice(li.getPrice() == null ? 0.0 : li.getPrice());
                item.setDiscountPct(li.getDiscountPct() == null ? 0.0 : Math.max(0.0, li.getDiscountPct()));
                item.setTaxPct(li.getTaxPct() == null ? 0.0 : Math.max(0.0, li.getTaxPct()));
                
                // Calculate item totals
                double base = item.getQuantity() * item.getPrice();
                double discount = base * (item.getDiscountPct() / 100.0);
                double exTax = base - discount;
                double tax = exTax * (item.getTaxPct() / 100.0);
                
                item.setAmountExTax(round2(exTax));
                item.setAmountIncTax(round2(exTax + tax));
                
                newItems.add(item);
            }
            
            if (invoice.getItems() == null) {
                invoice.setItems(newItems);
            } else {
                invoice.getItems().addAll(newItems);
            }
            
            // Re-save invoice to cascade items
            invoice = invoiceRepository.save(invoice);
        }

        // 6. Recalculate totals
        recalculateInvoiceTotals(invoice);

        Invoice saved = invoiceRepository.save(invoice);
        if (operator != null) {
            operationLogService.log(
                    operator.getEmail(),
                    operator.getName(),
                    "CREATE",
                    "invoice",
                    String.valueOf(saved.getId()),
                    "创建发票: " + saved.getCustomerName()
            );
        }
        return saved;
    }

    private void recalculateInvoiceTotals(Invoice invoice) {
        if (invoice.getItems() == null) return;
        
        double subtotalExTax = 0.0;
        double taxTotal = 0.0;
        
        for (InvoiceLineItem item : invoice.getItems()) {
             subtotalExTax += item.getAmountExTax();
             taxTotal += (item.getAmountIncTax() - item.getAmountExTax());
        }
        
        invoice.setSubtotalExTax(round2(subtotalExTax));
        invoice.setTaxTotal(round2(taxTotal));
        invoice.setGrandTotal(round2(subtotalExTax + taxTotal));
    }

    private Invoice createSimpleInvoice(CreateInvoiceRequest request, User operator) {
        Invoice invoice = new Invoice();
        String name = request.getCustomerName() != null ? request.getCustomerName().trim() : "";
        invoice.setCustomerName(name);
        LocalDate invDate = request.getInvoiceDate() != null ? request.getInvoiceDate() : LocalDate.now();
        invoice.setInvoiceDate(invDate);
        LocalDate due = request.getDueDate() != null ? request.getDueDate() : invDate.plusDays(30);
        invoice.setDueDate(due);
        String currency = request.getCurrency() != null ? request.getCurrency() : "CNY";
        invoice.setCurrency(currency);
        invoice.setTaxNumber(request.getTaxNumber());
        invoice.setPaymentReference(request.getPaymentReference());
        invoice.setActivities(request.getActivities() != null ? request.getActivities() : new ArrayList<>());
        invoice.setTerms(request.getTerms());
        invoice.setStatus(InvoiceStatus.DRAFT);

        double subtotalExTax = 0.0;
        double taxTotal = 0.0;

        if (request.getItems() != null) {
            for (CreateInvoiceRequest.LineItem li : request.getItems()) {
                InvoiceLineItem item = new InvoiceLineItem();
                item.setInvoice(invoice);
                item.setProductId(li.getProductId());
                item.setLabel(li.getLabel());
                item.setQuantity(li.getQuantity() == null ? 1 : Math.max(1, li.getQuantity()));
                item.setPrice(li.getPrice() == null ? 0.0 : li.getPrice());
                item.setDiscountPct(li.getDiscountPct() == null ? 0.0 : Math.max(0.0, li.getDiscountPct()));
                item.setTaxPct(li.getTaxPct() == null ? 0.0 : Math.max(0.0, li.getTaxPct()));

                double base = item.getQuantity() * item.getPrice();
                double discount = base * (item.getDiscountPct() / 100.0);
                double exTax = base - discount;
                double tax = exTax * (item.getTaxPct() / 100.0);

                item.setAmountExTax(round2(exTax));
                item.setAmountIncTax(round2(exTax + tax));

                subtotalExTax += exTax;
                taxTotal += tax;

                if (invoice.getItems() == null) {
                    invoice.setItems(new ArrayList<>());
                }
                invoice.getItems().add(item);
            }
        }

        invoice.setSubtotalExTax(round2(subtotalExTax));
        invoice.setTaxTotal(round2(taxTotal));
        invoice.setGrandTotal(round2(subtotalExTax + taxTotal));

        Invoice saved = invoiceRepository.save(invoice);
        if (operator != null) {
            operationLogService.log(
                    operator.getEmail(),
                    operator.getName(),
                    "CREATE",
                    "invoice",
                    String.valueOf(saved.getId()),
                    "创建发票: " + saved.getCustomerName()
            );
        }
        return saved;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    @Transactional
    public Invoice updateInvoice(Long id, UpdateInvoiceRequest request, User operator) {
        Invoice invoice = getInvoice(id);
        if (request.getCustomerName() != null) {
            invoice.setCustomerName(request.getCustomerName().trim());
        }
        if (request.getInvoiceDate() != null) {
            invoice.setInvoiceDate(request.getInvoiceDate());
        }
        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }
        if (request.getCurrency() != null) {
            invoice.setCurrency(request.getCurrency());
        }
        if (request.getTaxNumber() != null) {
            invoice.setTaxNumber(request.getTaxNumber());
        }
        if (request.getPaymentReference() != null) {
            invoice.setPaymentReference(request.getPaymentReference());
        }
        if (request.getActivities() != null) {
            invoice.setActivities(request.getActivities());
        }
        if (request.getTerms() != null) {
            invoice.setTerms(request.getTerms());
        }

        if (request.getItems() != null) {
            invoice.getItems().clear();
            double subtotalExTax = 0.0;
            double taxTotal = 0.0;
            for (UpdateInvoiceRequest.LineItem li : request.getItems()) {
                InvoiceLineItem item = new InvoiceLineItem();
                item.setInvoice(invoice);
                item.setProductId(li.getProductId());
                item.setLabel(li.getLabel());
                item.setQuantity(li.getQuantity() == null ? 1 : Math.max(1, li.getQuantity()));
                item.setPrice(li.getPrice() == null ? 0.0 : li.getPrice());
                item.setDiscountPct(li.getDiscountPct() == null ? 0.0 : Math.max(0.0, li.getDiscountPct()));
                item.setTaxPct(li.getTaxPct() == null ? 0.0 : Math.max(0.0, li.getTaxPct()));

                double base = item.getQuantity() * item.getPrice();
                double discount = base * (item.getDiscountPct() / 100.0);
                double exTax = base - discount;
                double tax = exTax * (item.getTaxPct() / 100.0);
                double incTax = exTax + tax;

                item.setAmountExTax(round2(exTax));
                item.setAmountIncTax(round2(incTax));

                invoice.getItems().add(item);
                subtotalExTax += exTax;
                taxTotal += tax;
            }
            invoice.setSubtotalExTax(round2(subtotalExTax));
            invoice.setTaxTotal(round2(taxTotal));
            invoice.setGrandTotal(round2(subtotalExTax + taxTotal));
        }

        Invoice saved = invoiceRepository.save(invoice);
        if (operator != null) {
            operationLogService.log(
                    operator.getEmail(),
                    operator.getName(),
                    "UPDATE",
                    "invoice",
                    String.valueOf(saved.getId()),
                    "更新发票: " + saved.getCustomerName()
            );
        }
        return saved;
    }



    @Data
    public static class CreateInvoiceRequest {
        @Schema(description = "Customer Monthly Bill ID")
        @JsonProperty("customer_monthly_bill_id")
        private Long customerMonthlyBillId;

        @Schema(description = "客户名称")
        @JsonProperty("customer_name")
        private String customerName;

        @Schema(description = "发票日期，格式：YYYY-MM-DD")
        @JsonProperty("invoice_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate invoiceDate;

        @Schema(description = "到期日期，格式：YYYY-MM-DD")
        @JsonProperty("due_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dueDate;

        @Schema(description = "币种，例如：USD、CNY")
        @JsonProperty("currency")
        private String currency;

        @Schema(description = "Tax Number")
        @JsonProperty("tax_number")
        private String taxNumber;

        @Schema(description = "付款参考")
        @JsonProperty("payment_reference")
        private String paymentReference;

        @Schema(description = "活动列表")
        @JsonProperty("activities")
        private List<String> activities;

        @Schema(description = "条款说明")
        @JsonProperty("terms")
        private String terms;

        @Schema(description = "行项目列表")
        @JsonProperty("items")
        private List<LineItem> items;

        @Data
        public static class LineItem {
            @Schema(description = "产品ID")
            @JsonProperty("product_id")
            private String productId;

            @Schema(description = "标签")
            @JsonProperty("label")
            private String label;

            @Schema(description = "数量")
            @JsonProperty("quantity")
            private Integer quantity;

            @Schema(description = "单价")
            @JsonProperty("price")
            private Double price;

            @Schema(description = "折扣%")
            @JsonProperty("discount_pct")
            private Double discountPct;

            @Schema(description = "税项%")
            @JsonProperty("tax_pct")
            private Double taxPct;
        }
    }

    @Data
    public static class UpdateInvoiceRequest {
        @JsonProperty("customer_name")
        private String customerName;
        @JsonProperty("invoice_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate invoiceDate;
        @JsonProperty("due_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dueDate;
        @JsonProperty("currency")
        private String currency;
        @JsonProperty("tax_number")
        private String taxNumber;
        @JsonProperty("payment_reference")
        private String paymentReference;
        @JsonProperty("activities")
        private List<String> activities;
        @JsonProperty("terms")
        private String terms;
        @JsonProperty("items")
        private List<LineItem> items;

        @Data
        public static class LineItem {
            @JsonProperty("product_id")
            private String productId;
            @JsonProperty("label")
            private String label;
            @JsonProperty("quantity")
            private Integer quantity;
            @JsonProperty("price")
            private Double price;
            @JsonProperty("discount_pct")
            private Double discountPct;
            @JsonProperty("tax_pct")
            private Double taxPct;
        }
    }
}
