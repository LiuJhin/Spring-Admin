package org.example.cloudopsadmin.service;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.entity.Account;
import org.example.cloudopsadmin.entity.Customer;
import org.example.cloudopsadmin.entity.Email;
import org.example.cloudopsadmin.entity.Payer;
import org.example.cloudopsadmin.entity.User;
import org.example.cloudopsadmin.repository.AccountRepository;
import org.example.cloudopsadmin.repository.CustomerRepository;
import org.example.cloudopsadmin.repository.EmailRepository;
import org.example.cloudopsadmin.repository.PayerRepository;
import org.example.cloudopsadmin.service.OperationLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Pattern AWS_ACCOUNT_ID_PATTERN = Pattern.compile("^\\d{12}$");

    private final AccountRepository accountRepository;
    private final PayerRepository payerRepository;
    private final CustomerRepository customerRepository;
    private final EmailRepository emailRepository;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    @Value("${jwt.secret}")
    private String encryptionSecret;

    public Page<Account> getAccountList(int page, int pageSize, String search, String vendor, String accountCategory, String accountSource, String label, String sortBy, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), StringUtils.hasText(sortBy) ? sortBy : "createdAt");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        Specification<Account> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            if (StringUtils.hasText(search)) {
                String likePattern = "%" + search.trim() + "%";
                Predicate nameLike = cb.like(root.get("accountName"), likePattern);
                Predicate uidLike = cb.like(root.get("uid"), likePattern);
                Predicate internalIdLike = cb.like(root.get("accountInternalId"), likePattern);
                predicates.add(cb.or(nameLike, uidLike, internalIdLike));
            }

            if (StringUtils.hasText(vendor)) {
                predicates.add(cb.equal(root.get("accountType"), vendor));
            }

            if (StringUtils.hasText(accountCategory)) {
                predicates.add(cb.equal(root.get("accountCategory"), accountCategory));
            }

            if (StringUtils.hasText(accountSource)) {
                predicates.add(cb.equal(root.get("accountSource"), accountSource));
            }

            if (StringUtils.hasText(label)) {
                predicates.add(cb.isMember(label, root.get("labels")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return accountRepository.findAll(spec, pageable);
    }

    public static final String SP_ACCOUNT_CATEGORY = "SP Account";

    public Page<Account> getSpAccountList(int page, int pageSize, String search, String vendor, String accountCategory, String accountSource, String label, String sortBy, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), StringUtils.hasText(sortBy) ? sortBy : "createdAt");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        Specification<Account> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            // Filter for SP accounts: isMonitoredSp is true OR accountCategory is "SP Account"
            Predicate isSpFlag = cb.equal(root.get("isMonitoredSp"), true);
            Predicate isSpCategory = cb.equal(root.get("accountCategory"), SP_ACCOUNT_CATEGORY);
            predicates.add(cb.or(isSpFlag, isSpCategory));

            if (StringUtils.hasText(search)) {
                String likePattern = "%" + search.trim() + "%";
                Predicate nameLike = cb.like(root.get("accountName"), likePattern);
                Predicate uidLike = cb.like(root.get("uid"), likePattern);
                Predicate internalIdLike = cb.like(root.get("accountInternalId"), likePattern);
                predicates.add(cb.or(nameLike, uidLike, internalIdLike));
            }

            if (StringUtils.hasText(vendor)) {
                predicates.add(cb.equal(root.get("accountType"), vendor));
            }

            if (StringUtils.hasText(accountCategory)) {
                predicates.add(cb.equal(root.get("accountCategory"), accountCategory));
            }

            if (StringUtils.hasText(accountSource)) {
                predicates.add(cb.equal(root.get("accountSource"), accountSource));
            }

            if (StringUtils.hasText(label)) {
                predicates.add(cb.isMember(label, root.get("labels")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return accountRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAccountDetail(String accountId, String uid) {
        Account account;
        if (StringUtils.hasText(accountId)) {
            account = accountRepository.findByAccountInternalId(accountId.trim())
                    .orElseThrow(() -> new ApiException(404, "账号不存在"));
        } else if (StringUtils.hasText(uid)) {
            account = accountRepository.findByUid(uid.trim())
                    .orElseThrow(() -> new ApiException(404, "账号不存在"));
        } else {
            throw new ApiException(400, "account_id 或 uid 必填其一");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("account_internal_id", account.getAccountInternalId());
        data.put("uid", account.getUid());
        data.put("monitor_email", account.getMonitorEmail());
        data.put("monitor_url", account.getMonitorUrl());
        data.put("account_name", account.getAccountName());
        
        // Fix mapping: accountType is Vendor, accountCategory is Account Type
        String mappedVendor = account.getAccountType();
        String mappedAccountType = account.getAccountCategory();
        // Handle legacy data where "Customer Account" was stored in vendor field
        if ("Customer Account".equals(mappedVendor)) {
            if (account.getUid() != null && account.getUid().matches("\\d{12}")) {
                mappedVendor = "AWS";
            }
            if (mappedAccountType == null) {
                mappedAccountType = "Customer Account";
            }
        }
        data.put("vendor", mappedVendor);
        data.put("account_type", mappedAccountType);
        
        data.put("account_category", account.getAccountCategory()); // Keep for backward compatibility if needed
        data.put("mfa_status", account.getMfaStatus());
        data.put("account_source", account.getAccountSource());
        data.put("account_attribution", account.getAccountAttribution());
        data.put("is_monitored_sp", account.getIsMonitoredSp());
        data.put("monitor_bill_group", account.getMonitorBillGroup());
        data.put("send_po", account.getSendPo());
        data.put("bound_credit_card", account.getBoundCreditCardMasked());
        data.put("bound_email", account.getBoundEmail());
        data.put("risk_discount", account.getRiskDiscount());
        data.put("cost_discount", account.getCostDiscount());
        data.put("remarks", account.getRemarks());
        data.put("is_submitted", account.getIsSubmitted());
        data.put("labels", account.getLabels());
        data.put("created_at", account.getCreatedAt() != null ? account.getCreatedAt().toString() : null);

        if (account.getPayer() != null) {
            Map<String, Object> payer = new HashMap<>();
            payer.put("payer_internal_id", account.getPayer().getPayerInternalId());
            payer.put("payer_id", account.getPayer().getPayerId());
            payer.put("payer_name", account.getPayer().getPayerName());
            data.put("payer", payer);
        } else {
            data.put("payer", null);
        }

        if (account.getCustomer() != null) {
            Map<String, Object> customer = new HashMap<>();
            customer.put("customer_id", account.getCustomer().getCustomerInternalId());
            customer.put("customer_name", account.getCustomer().getCustomerName());
            data.put("customer", customer);
        } else {
            data.put("customer", null);
        }

        if (account.getLinkedEmail() != null) {
            Map<String, Object> email = new HashMap<>();
            email.put("email_id", account.getLinkedEmail().getEmailInternalId());
            email.put("email_address", account.getLinkedEmail().getEmailAddress());
            data.put("email", email);
        } else {
            data.put("email", null);
        }

        return data;
    }

    @Transactional
    public Map<String, Object> updateAccount(UpdateAccountRequest request, User operator) {
        requireAccountManagePermission();

        String uid = requireText(request.getUid(), "uid");
        Account account = accountRepository.findByUid(uid)
                .orElseThrow(() -> new ApiException(404, "账号不存在"));

        // Update fields
        if (StringUtils.hasText(request.getMonitorEmail())) {
            account.setMonitorEmail(request.getMonitorEmail().trim());
        }
        
        String monitorUrl = sanitizeUrl(request.getMonitorUrl());
        if (StringUtils.hasText(monitorUrl)) {
            account.setMonitorUrl(monitorUrl);
        } else if (AWS_ACCOUNT_ID_PATTERN.matcher(uid).matches() && !StringUtils.hasText(account.getMonitorUrl())) {
             account.setMonitorUrl(buildAwsSigninUrl(uid));
        }

        if (StringUtils.hasText(request.getAccountName())) account.setAccountName(request.getAccountName().trim());
        if (StringUtils.hasText(request.getVendor())) account.setAccountType(request.getVendor().trim()); // vendor -> accountType
        if (StringUtils.hasText(request.getAccountType())) {
            account.setAccountCategory(request.getAccountType().trim());
            if (SP_ACCOUNT_CATEGORY.equals(request.getAccountType().trim())) {
                account.setIsMonitoredSp(true);
            }
        }
        if (request.getMfaStatus() != null) account.setMfaStatus(request.getMfaStatus());
        if (StringUtils.hasText(request.getAccountSource())) account.setAccountSource(request.getAccountSource().trim());
        if (StringUtils.hasText(request.getAccountAttribution())) account.setAccountAttribution(request.getAccountAttribution().trim());
        if (StringUtils.hasText(request.getBdName())) account.setBdName(request.getBdName().trim());
        if (request.getIsMonitoredSp() != null) account.setIsMonitoredSp(request.getIsMonitoredSp());
        if (request.getMonitorBillGroup() != null) account.setMonitorBillGroup(request.getMonitorBillGroup());
        if (request.getSendPo() != null) account.setSendPo(request.getSendPo());
        if (StringUtils.hasText(request.getBoundEmail())) account.setBoundEmail(request.getBoundEmail().trim());
        if (request.getRiskDiscount() != null) {
            validatePercentage(request.getRiskDiscount(), "risk_discount");
            account.setRiskDiscount(request.getRiskDiscount());
        }
        if (request.getCostDiscount() != null) {
            validatePercentage(request.getCostDiscount(), "cost_discount");
            account.setCostDiscount(request.getCostDiscount());
        }
        if (request.getRemarks() != null) account.setRemarks(request.getRemarks());
        if (request.getIsSubmitted() != null) account.setIsSubmitted(request.getIsSubmitted());
        if (request.getIsNew() != null) account.setIsNew(request.getIsNew());
        if (request.getLabels() != null) account.setLabels(request.getLabels());

        // Handle Credit Card
        String newCard = request.getBoundCreditCard();
        if (StringUtils.hasText(newCard) && !newCard.contains("*")) {
            account.setBoundCreditCardEncrypted(encrypt(newCard));
            account.setBoundCreditCardMasked(maskCreditCard(newCard));
        }

        // Handle Payer
        if (StringUtils.hasText(request.getPayerId())) {
            String payerId = request.getPayerId();
            // If currently linked payer is different or force update
            if (account.getPayer() == null || !payerId.equals(account.getPayer().getPayerInternalId())) {
                Payer payer = payerRepository.findByPayerInternalId(payerId)
                    .orElseGet(() -> payerRepository.findByPayerId(payerId)
                            .orElseThrow(() -> new ApiException(1002, "关联的 Payer 不存在")));
                account.setPayer(payer);
            }
        }

        // Handle Customer
        if (StringUtils.hasText(request.getCustomerId())) {
            String customerId = request.getCustomerId();
             if (account.getCustomer() == null || !customerId.equals(account.getCustomer().getCustomerInternalId())) {
                Customer customer = customerRepository.findByCustomerInternalId(customerId)
                        .orElseThrow(() -> new ApiException(1003, "关联的 Customer 不存在"));
                if (!isActive(customer.getStatus())) {
                    throw new ApiException(1003, "关联的 Customer 状态异常");
                }
                account.setCustomer(customer);
             }
        } else if (request.getCustomerId() == null) {
             // If explicit null/empty, maybe unlink? 
             // But request usually sends all fields. If empty string, ignore or unlink?
             // Assuming required fields logic handles validation, here optional.
        }

        // Handle Email
        // We need to resolve email again if ID or Address changed.
        // Simplified: if email_id provided, use it. If address provided, find or create.
        // For update, we might just update link.
        // Reusing resolveEmailAssociation is tricky because it takes AddAccountRequest.
        // I'll extract logic or duplicate for now to avoid refactoring AddAccountRequest.
        
        Email email = null;
        if (StringUtils.hasText(request.getEmailId())) {
             email = emailRepository.findByEmailInternalId(request.getEmailId())
                    .orElseThrow(() -> new ApiException(1004, "关联的 Email 不存在"));
            validateEmailUsable(email);
            checkEmailNotBound(email, uid);
        } else {
             String address = null;
             if (StringUtils.hasText(request.getEmailAddress())) address = request.getEmailAddress();
             else if (StringUtils.hasText(request.getBoundEmail())) address = request.getBoundEmail();
             else if (StringUtils.hasText(request.getMonitorEmail())) address = request.getMonitorEmail();

             if (StringUtils.hasText(address)) {
                 address = address.trim();
                 Optional<Email> existing = emailRepository.findByEmailAddressIgnoreCase(address);
                 if (existing.isPresent()) {
                     validateEmailUsable(existing.get());
                     checkEmailNotBound(existing.get(), uid);
                     email = existing.get();
                 } else {
                     // Create new email if not found?
                     // Similar to addAccount logic
                     email = new Email();
                     email.setEmailAddress(address);
                     email.setPassword(passwordEncoder.encode(randomPassword()));
                     email.setSource("API Creation");
                     email.setCategory("normal");
                     email.setStatus("active");
                     if (account.getPayer() != null) email.setPayer(account.getPayer());
                     email.setLinkedAccountUid(uid);
                     email.setEmailInternalId(generateEmailInternalId());
                     email = emailRepository.save(email);
                 }
             }
        }
        
        if (email != null) {
            account.setLinkedEmail(email);
            // Update email side link
            email.setLinkedAccountName(account.getAccountName());
            email.setLinkedAccountUid(account.getUid());
            emailRepository.save(email);
        }

        Account saved = accountRepository.save(account);

        if (operator != null) {
            operationLogService.log(
                    operator.getEmail(),
                    operator.getName(),
                    "UPDATE",
                    "account",
                    saved.getAccountInternalId(),
                    "更新账号: " + saved.getAccountName()
            );
        }
        
        // Return updated detail
        return getAccountDetail(saved.getAccountInternalId(), null);
    }

    @Transactional
    public Map<String, Object> addAccount(AddAccountRequest request, User operator) {
        requireAccountManagePermission();

        String uid = requireText(request.getUid(), "uid");
        if (uid.length() > 64) {
            throw new ApiException(400, "UID 长度不能超过 64");
        }

        if (accountRepository.existsByUid(uid)) {
            throw new ApiException(1001, "UID 已存在");
        }

        Double riskDiscount = requireNumber(request.getRiskDiscount(), "risk_discount");
        Double costDiscount = requireNumber(request.getCostDiscount(), "cost_discount");
        validatePercentage(riskDiscount, "risk_discount");
        validatePercentage(costDiscount, "cost_discount");

        String payerId = requireText(request.getPayerId(), "payer_id");
        Payer payer = payerRepository.findByPayerInternalId(payerId)
                .orElseGet(() -> payerRepository.findByPayerId(payerId)
                        .orElseThrow(() -> new ApiException(1002, "关联的 Payer 不存在")));

        Customer customer = null;
        if (StringUtils.hasText(request.getCustomerId())) {
            customer = customerRepository.findByCustomerInternalId(request.getCustomerId())
                    .orElseThrow(() -> new ApiException(1003, "关联的 Customer 不存在"));
            if (!isActive(customer.getStatus())) {
                throw new ApiException(1003, "关联的 Customer 状态异常");
            }
        }

        Email email = resolveEmailAssociation(request, payer, uid);

        String monitorEmail = requireText(request.getMonitorEmail(), "monitor_email");
        String boundEmail = requireText(request.getBoundEmail(), "bound_email");

        String accountName = requireText(request.getAccountName(), "account_name");
        
        // Fix mapping: request.vendor -> accountType (DB), request.account_type -> accountCategory (DB)
        String accountType = requireText(request.getVendor(), "vendor"); 
        String accountCategory = requireText(request.getAccountType(), "account_type");
        
        String accountSource = requireText(request.getAccountSource(), "account_source");
        String accountAttribution = requireText(request.getAccountAttribution(), "account_attribution");
        Boolean isMonitoredSp = requireBoolean(request.getIsMonitoredSp(), "is_monitored_sp");
        String monitorBillGroup = requireText(request.getMonitorBillGroup(), "monitor_bill_group");

        String monitorUrl = sanitizeUrl(request.getMonitorUrl());
        if (!StringUtils.hasText(monitorUrl)) {
            if (AWS_ACCOUNT_ID_PATTERN.matcher(uid).matches()) {
                monitorUrl = buildAwsSigninUrl(uid);
            } else {
                throw new ApiException(400, "monitor_url 必填（非 12 位 UID 无法自动生成）");
            }
        } else if (AWS_ACCOUNT_ID_PATTERN.matcher(uid).matches()) {
            monitorUrl = buildAwsSigninUrl(uid);
        }

        Account account = new Account();
        account.setAccountInternalId(generateAccountInternalId());
        account.setUid(uid);
        account.setMonitorEmail(monitorEmail);
        account.setMonitorUrl(monitorUrl);
        account.setAccountName(accountName);
        account.setAccountType(accountType);
        account.setAccountCategory(accountCategory);
        if (SP_ACCOUNT_CATEGORY.equals(accountCategory)) {
            isMonitoredSp = true;
        }
        account.setMfaStatus(request.getMfaStatus());
        account.setAccountSource(accountSource);
        account.setAccountAttribution(accountAttribution);
        account.setIsMonitoredSp(isMonitoredSp);
        account.setMonitorBillGroup(monitorBillGroup);
        account.setSendPo(request.getSendPo());
        account.setBoundEmail(boundEmail);
        account.setRiskDiscount(riskDiscount);
        account.setCostDiscount(costDiscount);
        account.setRemarks(request.getRemarks());
        account.setIsSubmitted(request.getIsSubmitted() != null ? request.getIsSubmitted() : false);
        account.setIsNew(request.getIsNew());
        account.setLabels(request.getLabels() != null ? request.getLabels() : List.of());

        String boundCreditCard = requireText(request.getBoundCreditCard(), "bound_credit_card");
        account.setBoundCreditCardEncrypted(encrypt(boundCreditCard));
        String boundCreditCardMasked = maskCreditCard(boundCreditCard);
        account.setBoundCreditCardMasked(boundCreditCardMasked);

        account.setPayer(payer);
        account.setCustomer(customer);
        account.setLinkedEmail(email);

        Account saved = accountRepository.save(account);

        if (email != null) {
            email.setLinkedAccountName(saved.getAccountName());
            email.setLinkedAccountUid(saved.getUid());
            emailRepository.save(email);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("account_id", saved.getAccountInternalId());
        data.put("uid", saved.getUid());
        data.put("account_name", saved.getAccountName());
        data.put("vendor", saved.getAccountType());
        data.put("account_type", saved.getAccountCategory());
        data.put("account_category", saved.getAccountCategory());
        data.put("mfa_status", saved.getMfaStatus());
        data.put("account_source", saved.getAccountSource());
        data.put("account_attribution", saved.getAccountAttribution());
        data.put("is_monitored_sp", saved.getIsMonitoredSp());
        data.put("is_new", saved.getIsNew());
        data.put("monitor_bill_group", saved.getMonitorBillGroup());
        data.put("send_po", saved.getSendPo());
        data.put("bound_credit_card", saved.getBoundCreditCardMasked());
        data.put("bound_email", saved.getBoundEmail());
        data.put("monitor_email", saved.getMonitorEmail());
        data.put("monitor_url", saved.getMonitorUrl());
        data.put("risk_discount", saved.getRiskDiscount());
        data.put("cost_discount", saved.getCostDiscount());
        data.put("remarks", saved.getRemarks());
        data.put("is_submitted", saved.getIsSubmitted());

        data.put("payer_id", payer.getPayerInternalId());
        data.put("payer_name", payer.getPayerName());

        if (customer != null) {
            data.put("customer_id", customer.getCustomerInternalId());
            data.put("customer_name", customer.getCustomerName());
        } else {
            data.put("customer_id", null);
            data.put("customer_name", null);
        }

        if (email != null) {
            data.put("email_id", email.getEmailInternalId());
            data.put("email_address", email.getEmailAddress());
        } else {
            data.put("email_id", null);
            data.put("email_address", null);
        }

        data.put("created_at", saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null);

        if (operator != null) {
            operationLogService.log(
                    operator.getEmail(),
                    operator.getName(),
                    "CREATE",
                    "account",
                    saved.getAccountInternalId(),
                    "新增账号: " + saved.getAccountName()
            );
        }

        return data;
    }

    private Email resolveEmailAssociation(AddAccountRequest request, Payer payer, String uid) {
        if (StringUtils.hasText(request.getEmailId())) {
            Email email = emailRepository.findByEmailInternalId(request.getEmailId())
                    .orElseThrow(() -> new ApiException(1004, "关联的 Email 不存在"));
            validateEmailUsable(email);
            checkEmailNotBound(email, null);
            return email;
        }

        String address = null;
        if (StringUtils.hasText(request.getEmailAddress())) {
            address = request.getEmailAddress();
        } else if (StringUtils.hasText(request.getBoundEmail())) {
            address = request.getBoundEmail();
        } else if (StringUtils.hasText(request.getMonitorEmail())) {
            address = request.getMonitorEmail();
        }

        if (!StringUtils.hasText(address)) {
            return null;
        }

        address = address.trim();
        Optional<Email> existing = emailRepository.findByEmailAddressIgnoreCase(address);
        if (existing.isPresent()) {
            validateEmailUsable(existing.get());
            checkEmailNotBound(existing.get(), null);
            return existing.get();
        }

        Email email = new Email();
        email.setEmailAddress(address);
        email.setPassword(passwordEncoder.encode(randomPassword()));
        email.setSource("API Creation");
        email.setCategory("normal");
        email.setStatus("active");
        email.setPayer(payer);
        email.setLinkedAccountUid(uid);
        email.setEmailInternalId(generateEmailInternalId());
        return emailRepository.save(email);
    }

    private void validateEmailUsable(Email email) {
        if (!isActive(email.getStatus())) {
            throw new ApiException(1004, "关联的 Email 状态异常");
        }
        if ("secondary".equalsIgnoreCase(email.getCategory())) {
            throw new ApiException(1004, "关联的 Email 类型不支持（secondary）");
        }
    }

    private void checkEmailNotBound(Email email, String currentAccountUid) {
        boolean isBound;
        if (currentAccountUid == null) {
            // New account creation
            isBound = accountRepository.existsByLinkedEmail(email);
        } else {
            // Update existing account
            isBound = accountRepository.existsByLinkedEmailAndUidNot(email, currentAccountUid);
        }

        if (isBound) {
            throw new ApiException(1004, "该邮箱已被其他账号绑定");
        }
    }

    private void requireAccountManagePermission() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            throw new ApiException(1005, "无账号管理权限");
        }
        boolean ok = auth.getAuthorities().stream().anyMatch(a -> "ACCOUNT_MANAGE".equals(a.getAuthority()));
        if (!ok) {
            throw new ApiException(1005, "无账号管理权限");
        }
    }

    private boolean isActive(String status) {
        return "active".equalsIgnoreCase(status);
    }

    private void validatePercentage(Double value, String field) {
        if (value < 0.0 || value > 100.0) {
            throw new ApiException(400, field + " 必须在 0.0 ~ 100.0 范围内");
        }
    }

    private String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(400, field + " 必填");
        }
        return value.trim();
    }

    private Double requireNumber(Double value, String field) {
        if (value == null) {
            throw new ApiException(400, field + " 必填");
        }
        return value;
    }

    private Boolean requireBoolean(Boolean value, String field) {
        if (value == null) {
            throw new ApiException(400, field + " 必填");
        }
        return value;
    }

    private String sanitizeUrl(String url) {
        if (url == null) return null;
        String s = url.trim();
        if (s.length() >= 2 && s.startsWith("`") && s.endsWith("`")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private String buildAwsSigninUrl(String uid) {
        return "https://" + uid + ".signin.aws.amazon.com/console";
    }

    private synchronized String generateAccountInternalId() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "acc_" + today + "_";
        String pattern = prefix + "%";
        return accountRepository.findLastAccountInternalId(pattern)
                .map(lastId -> {
                    String suffix = lastId.substring(prefix.length());
                    int nextNum = Integer.parseInt(suffix) + 1;
                    return prefix + String.format("%03d", nextNum);
                })
                .orElse(prefix + "001");
    }

    private synchronized String generateEmailInternalId() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "email_" + today + "_";
        String pattern = prefix + "%";
        return emailRepository.findLastEmailInternalId(pattern)
                .map(lastId -> {
                    String suffix = lastId.substring(prefix.length());
                    int nextNum = Integer.parseInt(suffix) + 1;
                    return prefix + String.format("%03d", nextNum);
                })
                .orElse(prefix + "001");
    }

    private String randomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String maskCreditCard(String value) {
        String digits = value.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "************";
        }
        String last4 = digits.substring(digits.length() - 4);
        return "************" + last4;
    }

    private String encrypt(String plaintext) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(encryptionSecret.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new ApiException(500, "加密失败");
        }
    }

    @Data
    @Schema(description = "Update Account Request")
    public static class UpdateAccountRequest {
        private String uid;
        private String monitor_email;
        private String monitor_url;
        private String account_name;
        private String account_type; // Maps to accountCategory
        private String vendor; // Maps to accountType
        private Object mfa_status; // Boolean or String
        private String account_source;
        private String account_attribution;
        private String bd_name;
        
        @JsonProperty("is_monitored_sp")
        @JsonAlias("isMonitoredSp")
        private Boolean is_monitored_sp;
        
        @JsonProperty("monitor_bill_group")
        @JsonAlias("monitorBillGroup")
        private Object monitor_bill_group; // Boolean or String
        
        @JsonProperty("send_po")
        @JsonAlias("sendPo")
        private Boolean send_po;
        
        private String bound_credit_card;
        private String bound_email;
        private Double risk_discount;
        private Double cost_discount;
        private String remarks;
        private Boolean is_submitted;
        private Boolean is_new;
        private String payer_id;
        private String customer_id;
        private String email_id;
        private String email_address;
        private List<String> labels;

        public String getUid() { return uid; }
        public String getMonitorEmail() { return monitor_email; }
        public String getMonitorUrl() { return monitor_url; }
        public String getAccountName() { return account_name; }
        public String getAccountType() { return account_type; }
        public String getVendor() { return vendor; }
        
        public String getMfaStatus() {
            if (mfa_status == null) return null;
            if (mfa_status instanceof Boolean) {
                return ((Boolean) mfa_status) ? "true" : "false";
            }
            return mfa_status.toString();
        }

        public String getAccountSource() { return account_source; }
        public String getAccountAttribution() { return account_attribution; }
        public String getBdName() { return bd_name; }
        public Boolean getIsMonitoredSp() { return is_monitored_sp; }
        
        public String getMonitorBillGroup() {
            if (monitor_bill_group == null) return null;
            if (monitor_bill_group instanceof Boolean) {
                return ((Boolean) monitor_bill_group) ? "true" : "false";
            }
            return monitor_bill_group.toString();
        }

        public Boolean getSendPo() { return send_po; }
        public String getBoundCreditCard() { return bound_credit_card; }
        public String getBoundEmail() { return bound_email; }
        public Double getRiskDiscount() { return risk_discount; }
        public Double getCostDiscount() { return cost_discount; }
        public String getRemarks() { return remarks; }
        public Boolean getIsSubmitted() { return is_submitted; }
        public Boolean getIsNew() { return is_new; }
        public String getPayerId() { return payer_id; }
        public String getCustomerId() { return customer_id; }
        public String getEmailId() { return email_id; }
        public String getEmailAddress() { return email_address; }
        public List<String> getLabels() { return labels; }
    }

    @Data
    @Schema(description = "Add Account Request")
    public static class AddAccountRequest {
        private String uid;
        private String monitor_email;
        private String monitor_url;
        private String account_name;
        private String vendor; // Maps to accountType
        private String account_type; // Maps to accountCategory
        private String account_category;
        private String mfa_status;
        private String account_source;
        private String account_attribution;
        
        @JsonProperty("is_monitored_sp")
        @JsonAlias("isMonitoredSp")
        private Boolean is_monitored_sp;
        
        @JsonProperty("monitor_bill_group")
        @JsonAlias("monitorBillGroup")
        private String monitor_bill_group;
        
        @JsonProperty("send_po")
        @JsonAlias("sendPo")
        private Boolean send_po;
        
        private String bound_credit_card;
        private String bound_email;
        private Double risk_discount;
        private Double cost_discount;
        private String remarks;
        private Boolean is_submitted;
        private Boolean is_new;
        private String payer_id;
        private String customer_id;
        private String email_id;
        private String email_address;
        private List<String> labels;

        public String getUid() { return uid; }
        public String getMonitorEmail() { return monitor_email; }
        public String getMonitorUrl() { return monitor_url; }
        public String getAccountName() { return account_name; }
        public String getVendor() { return vendor; }
        public String getAccountType() { return account_type; }
        public String getAccountCategory() { return account_category; }
        public String getMfaStatus() { return mfa_status; }
        public String getAccountSource() { return account_source; }
        public String getAccountAttribution() { return account_attribution; }
        public Boolean getIsMonitoredSp() { return is_monitored_sp; }
        public String getMonitorBillGroup() { return monitor_bill_group; }
        public Boolean getSendPo() { return send_po; }
        public String getBoundCreditCard() { return bound_credit_card; }
        public String getBoundEmail() { return bound_email; }
        public Double getRiskDiscount() { return risk_discount; }
        public Double getCostDiscount() { return cost_discount; }
        public String getRemarks() { return remarks; }
        public Boolean getIsSubmitted() { return is_submitted; }
        public Boolean getIsNew() { return is_new; }
        public String getPayerId() { return payer_id; }
        public String getCustomerId() { return customer_id; }
        public String getEmailId() { return email_id; }
        public String getEmailAddress() { return email_address; }
        public List<String> getLabels() { return labels; }
    }

    public static class ApiException extends RuntimeException {
        private final int code;

        public ApiException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
