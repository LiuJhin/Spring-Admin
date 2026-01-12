package org.example.cloudopsadmin.service;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.entity.Email;
import org.example.cloudopsadmin.entity.Payer;
import org.example.cloudopsadmin.repository.EmailRepository;
import org.example.cloudopsadmin.repository.PayerRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailRepository emailRepository;
    private final PayerRepository payerRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional(readOnly = true)
    public Page<Email> getEmailList(int page, int pageSize, String search, String category, String status, String label, String sortBy, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder == null ? "ASC" : sortOrder), 
                            sortBy == null || sortBy.isEmpty() ? "id" : sortBy);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        Specification<Email> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String searchLike = "%" + search + "%";
                Predicate emailAddress = cb.like(root.get("emailAddress"), searchLike);
                Predicate emailInternalId = cb.like(root.get("emailInternalId"), searchLike);
                Predicate source = cb.like(root.get("source"), searchLike);
                Predicate remarks = cb.like(root.get("remarks"), searchLike);
                predicates.add(cb.or(emailAddress, emailInternalId, source, remarks));
            }

            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category));
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

        return emailRepository.findAll(spec, pageable);
    }

    @Transactional
    public Email createEmail(CreateEmailRequest request) {
        String emailAddress = request.getEmailAddress();
        if (!StringUtils.hasText(emailAddress)) {
            throw new IllegalArgumentException("email_address 必填");
        }
        emailAddress = emailAddress.trim();

        if (emailRepository.existsByEmailAddressIgnoreCase(emailAddress)) {
            throw new IllegalArgumentException("Email address already exists: " + emailAddress);
        }

        Email email = new Email();
        email.setEmailAddress(emailAddress);
        email.setPassword(passwordEncoder.encode(request.getPassword()));
        email.setSource(request.getSource());
        email.setCategory(request.getCategory());
        email.setStatus(request.getStatus() != null ? request.getStatus() : "active");
        email.setLabels(request.getLabels() != null ? request.getLabels() : List.of());
        email.setRemarks(request.getRemarks());
        
        // Optional fields
        email.setCreditCardLast4(request.getCreditCardLast4());
        email.setIsSpAccount(request.getIsSpAccount() != null ? request.getIsSpAccount() : false);
        email.setLinkedAccountName(request.getLinkedAccountName());
        email.setLinkedAccountUid(request.getLinkedAccountUid());
        email.setEnableForwarding(request.getEnableForwarding() != null ? request.getEnableForwarding() : false);

        // 2. Handle Parent Email (for secondary)
        if ("secondary".equalsIgnoreCase(request.getCategory())) {
            Email parent = null;
            if (StringUtils.hasText(request.getParentEmailId())) {
                parent = emailRepository.findByEmailInternalId(request.getParentEmailId())
                        .orElseThrow(() -> new IllegalArgumentException("Parent email not found by ID: " + request.getParentEmailId()));
            } else if (StringUtils.hasText(request.getParentEmailAddress())) {
                String parentAddress = request.getParentEmailAddress().trim();
                parent = emailRepository.findByEmailAddressIgnoreCase(parentAddress)
                        .orElseThrow(() -> new IllegalArgumentException("Parent email not found by address: " + parentAddress));
            } else {
                throw new IllegalArgumentException("For secondary emails, parent_email_id or parent_email_address is required");
            }

            if (!"primary".equalsIgnoreCase(parent.getCategory())) {
                throw new IllegalArgumentException("Parent email must be of category 'primary'");
            }
            email.setParentEmail(parent);
        }

        // 3. Handle Payer
        if (StringUtils.hasText(request.getPayerId())) {
            Payer payer = payerRepository.findByPayerId(request.getPayerId())
                    .orElseThrow(() -> new IllegalArgumentException("Payer not found: " + request.getPayerId()));
            email.setPayer(payer);
        }

        // 4. Generate Internal ID
        email.setEmailInternalId(generateEmailInternalId());

        return emailRepository.save(email);
    }

    private synchronized String generateEmailInternalId() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "email_" + today + "_";
        
        // Find last ID for today to increment
        String pattern = prefix + "%";
        return emailRepository.findLastEmailInternalId(pattern)
                .map(lastId -> {
                    String suffix = lastId.substring(prefix.length());
                    int nextNum = Integer.parseInt(suffix) + 1;
                    return prefix + String.format("%03d", nextNum);
                })
                .orElse(prefix + "001");
    }

    @lombok.Data
    @Schema(description = "Create Email Request")
    public static class CreateEmailRequest {
        @Schema(description = "Email address", example = "support@example.com")
        private String email_address;
        
        @Schema(description = "Password", example = "TempPass123!")
        private String password;
        
        @Schema(description = "Source", example = "AWS Management Console")
        private String source;
        
        @Schema(description = "Category (primary/secondary)", example = "secondary")
        private String category;
        
        @Schema(description = "Parent Email ID (required for secondary)", example = "email_20260108_001")
        private String parent_email_id;
        
        @Schema(description = "Parent Email Address (alternative to ID)", example = "admin@example.com")
        private String parent_email_address;
        
        @Schema(description = "Credit Card Last 4 Digits", example = "1234")
        private String credit_card_last4;
        
        @Schema(description = "Is SP Account", example = "false")
        private Boolean is_sp_account;
        
        @Schema(description = "Payer ID", example = "payer_001")
        private String payer_id;
        
        @Schema(description = "Linked Account Name", example = "acme-prod")
        private String linked_account_name;
        
        @Schema(description = "Linked Account UID", example = "123456789012")
        private String linked_account_uid;
        
        @Schema(description = "Enable Forwarding", example = "true")
        private Boolean enable_forwarding;
        
        @Schema(description = "Status", example = "active")
        private String status;
        
        @Schema(description = "Labels", example = "[\"support\", \"billing\"]")
        private List<String> labels;
        
        @Schema(description = "Remarks", example = "Support email")
        private String remarks;

        // Getters to bridge JSON naming (snake_case) to Java fields if needed, 
        // or just use standard getters/setters and Jackson will map if we use @JsonProperty or matching names.
        // For simplicity in Service, I'll provide standard camelCase getters that delegate to snake_case fields or rename fields to camelCase and use @JsonProperty.
        // Actually, let's just use getters that match the logic above.
        
        public String getEmailAddress() { return email_address; }
        public String getPassword() { return password; }
        public String getSource() { return source; }
        public String getCategory() { return category; }
        public String getParentEmailId() { return parent_email_id; }
        public String getParentEmailAddress() { return parent_email_address; }
        public String getCreditCardLast4() { return credit_card_last4; }
        public Boolean getIsSpAccount() { return is_sp_account; }
        public String getPayerId() { return payer_id; }
        public String getLinkedAccountName() { return linked_account_name; }
        public String getLinkedAccountUid() { return linked_account_uid; }
        public Boolean getEnableForwarding() { return enable_forwarding; }
        public String getStatus() { return status; }
        public List<String> getLabels() { return labels; }
        public String getRemarks() { return remarks; }
    }
}
