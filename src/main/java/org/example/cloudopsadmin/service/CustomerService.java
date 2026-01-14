package org.example.cloudopsadmin.service;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.entity.Customer;
import org.example.cloudopsadmin.entity.CustomerUid;
import org.example.cloudopsadmin.entity.Payer;
import org.example.cloudopsadmin.entity.User;
import org.example.cloudopsadmin.service.OperationLogService;
import org.example.cloudopsadmin.repository.CustomerRepository;
import org.example.cloudopsadmin.repository.PayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PayerRepository payerRepository;
    private final OperationLogService operationLogService;

    @Transactional(readOnly = true)
    public Page<Customer> getCustomerList(int page, int pageSize, String search, String status, String label, String sortBy, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder == null ? "ASC" : sortOrder), 
                            sortBy == null || sortBy.isEmpty() ? "id" : sortBy);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String searchLike = "%" + search + "%";
                Predicate customerName = cb.like(root.get("customerName"), searchLike);
                Predicate email = cb.like(root.get("email"), searchLike);
                Predicate customerInternalId = cb.like(root.get("customerInternalId"), searchLike);
                Predicate company = cb.like(root.get("company"), searchLike);
                predicates.add(cb.or(customerName, email, customerInternalId, company));
            }

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(label)) {
                 predicates.add(cb.isMember(label, root.get("labels")));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return customerRepository.findAll(spec, pageable);
    }

    @Transactional
    public Customer createCustomer(CreateCustomerRequest request, User operator) {
        String customerName = request.getCustomerName();
        if (!StringUtils.hasText(customerName)) {
            throw new IllegalArgumentException("customer_name 必填");
        }

        String email = request.getEmail();
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("email 必填");
        }

        if (customerRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("email 已存在: " + email);
        }

        if (customerRepository.existsByCustomerNameIgnoreCase(customerName.trim())) {
            throw new IllegalArgumentException("customer_name 已存在: " + customerName);
        }

        // 1. Create Customer
        Customer customer = new Customer();
        customer.setCustomerName(customerName.trim());
        customer.setEmail(email.trim());
        
        // Handle Payer
        if (StringUtils.hasText(request.getPayerId())) {
            // Assuming request.getPayerId() is the internal ID (e.g., payer_20260108_003) or the business ID (12 chars)
            // The request example shows "payer_20260108_003", which matches our internal ID format.
            // But let's check both or assume one. Our Payer entity has `payerInternalId` and `payerId`.
            // Let's try to find by internal ID first, then payer ID.
            Payer payer = payerRepository.findByPayerInternalId(request.getPayerId())
                    .orElseGet(() -> payerRepository.findByPayerId(request.getPayerId())
                            .orElseThrow(() -> new IllegalArgumentException("Payer not found: " + request.getPayerId())));
            customer.setPayer(payer);
        }

        customer.setPermissions(request.getPermissions() != null ? request.getPermissions() : new ArrayList<>());
        customer.setOriginalBillingPercentage(request.getOriginalBillingPercentage());
        customer.setProductsUsed(request.getProductsUsed() != null ? request.getProductsUsed() : new ArrayList<>());
        customer.setDeliveryTime(request.getDeliveryTime());
        customer.setCompany(request.getCompany());
        customer.setStatus(request.getStatus() != null ? request.getStatus() : "active");
        customer.setLabels(request.getLabels() != null ? request.getLabels() : new ArrayList<>());
        customer.setRemarks(request.getRemarks());

        // Generate Internal ID
        customer.setCustomerInternalId(generateCustomerInternalId());

        // 2. Handle UIDs (optional)
        if (request.getUids() != null && !request.getUids().isEmpty()) {
            java.util.Set<String> uniqueKeys = new java.util.HashSet<>();
            for (CustomerUidRequest uidReq : request.getUids()) {
                if (uidReq == null) {
                    throw new IllegalArgumentException("uids 不能包含空元素");
                }
                if (!StringUtils.hasText(uidReq.getUid())) {
                    throw new IllegalArgumentException("uids.uid 必填");
                }
                if (!StringUtils.hasText(uidReq.getUidType())) {
                    throw new IllegalArgumentException("uids.uid_type 必填");
                }
                String key = uidReq.getUidType().trim() + ":" + uidReq.getUid().trim();
                if (!uniqueKeys.add(key)) {
                    throw new IllegalArgumentException("uids 存在重复: " + key);
                }
            }
            List<CustomerUid> customerUids = request.getUids().stream().map(uidReq -> {
                CustomerUid uid = new CustomerUid();
                uid.setUid(uidReq.getUid().trim());
                uid.setUidType(uidReq.getUidType().trim());
                uid.setIsPrimary(uidReq.getIsPrimary());
                uid.setDescription(uidReq.getDescription());
                uid.setCustomer(customer);
                return uid;
            }).collect(Collectors.toList());
            customer.setUids(customerUids);
        }

        Customer saved = customerRepository.save(customer);
        if (operator != null) {
            operationLogService.log(
                    operator.getEmail(),
                    operator.getName(),
                    "CREATE",
                    "customer",
                    saved.getCustomerInternalId(),
                    "创建客户: " + saved.getCustomerName()
            );
        }
        return saved;
    }

    private synchronized String generateCustomerInternalId() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "cus_" + today + "_";
        
        String pattern = prefix + "%";
        return customerRepository.findLastCustomerInternalId(pattern)
                .map(lastId -> {
                    String suffix = lastId.substring(prefix.length());
                    int nextNum = Integer.parseInt(suffix) + 1;
                    return prefix + String.format("%03d", nextNum);
                })
                .orElse(prefix + "001");
    }

    @Data
    @Schema(description = "Create Customer Request")
    public static class CreateCustomerRequest {
        @Schema(description = "Customer Name", example = "腾讯云科技")
        private String customer_name;

        @Schema(description = "Email", example = "admin@tencent.com")
        private String email;

        @Schema(description = "Payer ID", example = "payer_20260108_003")
        private String payer_id;

        @Schema(description = "Permissions", example = "[\"admin\", \"billing_view\"]")
        private List<String> permissions;

        @Schema(description = "Original Billing Percentage", example = "15.5")
        private Double original_billing_percentage;

        @Schema(description = "Products Used", example = "[\"AWS\", \"Azure\", \"Aliyun\"]")
        private List<String> products_used;

        @Schema(description = "Delivery Time", example = "2025-12-20T00:00:00Z")
        private LocalDateTime delivery_time;

        @Schema(description = "Company", example = "腾讯云科技有限公司")
        private String company;

        @Schema(description = "Status", example = "active")
        private String status;

        @Schema(description = "Labels", example = "[\"vip\", \"strategic\"]")
        private List<String> labels;

        @Schema(description = "Remarks", example = "战略大客户")
        private String remarks;

        @Schema(description = "UIDs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private List<CustomerUidRequest> uids;

        // Getters for snake_case to camelCase mapping
        public String getCustomerName() { return customer_name; }
        public String getEmail() { return email; }
        public String getPayerId() { return payer_id; }
        public List<String> getPermissions() { return permissions; }
        public Double getOriginalBillingPercentage() { return original_billing_percentage; }
        public List<String> getProductsUsed() { return products_used; }
        public LocalDateTime getDeliveryTime() { return delivery_time; }
        public String getCompany() { return company; }
        public String getStatus() { return status; }
        public List<String> getLabels() { return labels; }
        public String getRemarks() { return remarks; }
        public List<CustomerUidRequest> getUids() { return uids; }
    }

    @Data
    @Schema(description = "Customer UID Request")
    public static class CustomerUidRequest {
        @Schema(description = "UID", example = "123456789012")
        private String uid;

        @Schema(description = "UID Type", example = "AWS_ACCOUNT_ID")
        private String uid_type;

        @Schema(description = "Is Primary", example = "true")
        private Boolean is_primary;

        @Schema(description = "Description", example = "Main Account")
        private String description;

        // Getters
        public String getUid() { return uid; }
        public String getUidType() { return uid_type; }
        public Boolean getIsPrimary() { return is_primary; }
        public String getDescription() { return description; }
    }
}
