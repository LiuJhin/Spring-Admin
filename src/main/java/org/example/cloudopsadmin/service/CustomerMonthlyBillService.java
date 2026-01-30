package org.example.cloudopsadmin.service;

import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.entity.Account;
import org.example.cloudopsadmin.entity.Customer;
import org.example.cloudopsadmin.entity.CustomerMonthlyBill;
import org.example.cloudopsadmin.repository.AccountRepository;
import org.example.cloudopsadmin.repository.CustomerMonthlyBillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerMonthlyBillService {

    private final CustomerMonthlyBillRepository customerMonthlyBillRepository;
    private final AccountRepository accountRepository;

    public Page<CustomerMonthlyBill> getMonthlyBillList(
            int page,
            int pageSize,
            String customerName,
            String linkedAccountUid,
            String month,
            String sortOrder
    ) {
        String targetMonth = StringUtils.hasText(month) ? month.trim() : DateTimeFormatter.ofPattern("yyyy-MM").format(LocalDate.now());
        ensureMonthRecords(targetMonth);

        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), "totalBill");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        Specification<CustomerMonthlyBill> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Join with account to ensure we only display bills for existing accounts
            root.join("account");

            if (StringUtils.hasText(customerName)) {
                predicates.add(cb.like(root.get("customerName"), "%" + customerName.trim() + "%"));
            }

            if (StringUtils.hasText(linkedAccountUid)) {
                predicates.add(cb.equal(root.get("linkedAccountUid"), linkedAccountUid.trim()));
            }

            if (StringUtils.hasText(targetMonth)) {
                predicates.add(cb.equal(root.get("month"), targetMonth));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return customerMonthlyBillRepository.findAll(spec, pageable);
    }

    private void ensureMonthRecords(String month) {
        List<Account> accounts = accountRepository.findAll();
        for (Account account : accounts) {
            String uid = account.getUid();
            Optional<CustomerMonthlyBill> existing = customerMonthlyBillRepository.findOne((root, query, cb) -> cb.and(
                    cb.equal(root.get("month"), month),
                    cb.equal(root.get("linkedAccountUid"), uid)
            ));
            
            if (existing.isPresent()) {
                CustomerMonthlyBill bill = existing.get();
                // Only sync if not invoiced/finalized, OR if the vendor is invalid (fix dirty data)
                boolean isInvalidVendor = "Customer Account".equals(bill.getCloudVendor());
                if (!Boolean.TRUE.equals(bill.getIsInvoiced()) || isInvalidVendor) {
                    boolean changed = false;
                    
                    // Sync Cloud Vendor using accountCategory (which stores Vendor) instead of accountType (which stores Category)
                    // Also handle the case where we need to fix "Customer Account" -> "AWS" (or actual vendor)
                    String normalizedVendor = normalizeCloudVendor(account.getAccountCategory());
                    
                    // Fallback to accountType if accountCategory is empty, just in case
                    if ("AWS".equals(normalizedVendor) && !StringUtils.hasText(account.getAccountCategory())) {
                        normalizedVendor = normalizeCloudVendor(account.getAccountType());
                    }

                    if (!normalizedVendor.equals(bill.getCloudVendor())) {
                        bill.setCloudVendor(normalizedVendor);
                        changed = true;
                    }

                    // Sync Customer Name
                    Customer customer = account.getCustomer();
                    String currentCustomerName = (customer != null && StringUtils.hasText(customer.getCustomerName()))
                            ? customer.getCustomerName()
                            : account.getAccountName();
                    
                    if (!currentCustomerName.equals(bill.getCustomerName())) {
                        bill.setCustomerName(currentCustomerName);
                        changed = true;
                    }
                    
                    // Sync Account Reference if missing
                    if (bill.getAccount() == null) {
                        bill.setAccount(account);
                        changed = true;
                    }
                    
                    // Sync Customer Reference
                    if (bill.getCustomer() != customer) { // Reference check is okay for managed entities, but maybe ID check is safer
                         // Ideally check IDs or nulls
                         Long oldCustId = bill.getCustomer() == null ? null : bill.getCustomer().getId();
                         Long newCustId = customer == null ? null : customer.getId();
                         if (oldCustId != null && !oldCustId.equals(newCustId) || (oldCustId == null && newCustId != null)) {
                             bill.setCustomer(customer);
                             changed = true;
                         }
                    }

                    if (changed) {
                        customerMonthlyBillRepository.save(bill);
                    }
                }
                continue;
            }
            
            CustomerMonthlyBill bill = new CustomerMonthlyBill();
            bill.setMonth(month);
            
            // Use accountCategory for Vendor
            String normalizedVendor = normalizeCloudVendor(account.getAccountCategory());
            if ("AWS".equals(normalizedVendor) && !StringUtils.hasText(account.getAccountCategory())) {
                normalizedVendor = normalizeCloudVendor(account.getAccountType());
            }
            bill.setCloudVendor(normalizedVendor);
            
            Customer customer = account.getCustomer();
            String customerName = (customer != null && org.springframework.util.StringUtils.hasText(customer.getCustomerName()))
                    ? customer.getCustomerName()
                    : account.getAccountName();
            bill.setCustomerName(customerName);
            bill.setLinkedAccountUid(uid);
            bill.setOriginalBillingPercentage(customer != null ? customer.getOriginalBillingPercentage() : null);
            bill.setCostDiscountPercentage(account.getCostDiscount());
            bill.setAccount(account);
            bill.setCustomer(customer);
            bill.setIsInvoiced(false);
            bill.setInvoiceStatus(org.example.cloudopsadmin.common.InvoiceStatus.DRAFT);
            customerMonthlyBillRepository.save(bill);
        }
    }

    public Optional<CustomerMonthlyBill> findPreviousMonthBill(String currentMonth, String uid) {
        LocalDate parsed = LocalDate.parse(currentMonth + "-01");
        LocalDate prev = parsed.minusMonths(1);
        String prevMonth = DateTimeFormatter.ofPattern("yyyy-MM").format(prev);
        return customerMonthlyBillRepository.findOne((root, query, cb) -> cb.and(
                cb.equal(root.get("month"), prevMonth),
                cb.equal(root.get("linkedAccountUid"), uid)
        ));
    }

    public List<CustomerMonthlyBill> listBillsByFilters(
            String month,
            String customerName,
            String linkedAccountUid,
            String cloudVendor
    ) {
        String targetMonth = StringUtils.hasText(month) ? month.trim() : DateTimeFormatter.ofPattern("yyyy-MM").format(LocalDate.now());
        ensureMonthRecords(targetMonth);
        Specification<CustomerMonthlyBill> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Ensure we join account here too? Maybe not necessary for all lists, but for consistency let's do it if it's for display.
            // But this method seems to be used for export or internal logic.
            // Let's keep it simple and only modify the main list method unless requested.
            
            if (StringUtils.hasText(targetMonth)) {
                predicates.add(cb.equal(root.get("month"), targetMonth));
            }
            if (StringUtils.hasText(customerName)) {
                predicates.add(cb.like(root.get("customerName"), "%" + customerName.trim() + "%"));
            }
            if (StringUtils.hasText(linkedAccountUid)) {
                predicates.add(cb.equal(root.get("linkedAccountUid"), linkedAccountUid.trim()));
            }
            if (StringUtils.hasText(cloudVendor)) {
                predicates.add(cb.equal(root.get("cloudVendor"), cloudVendor.trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return customerMonthlyBillRepository.findAll(spec);
    }

    public List<CustomerMonthlyBill> listBillsByYear(int year) {
        String yearPrefix = String.valueOf(year) + "-%";
        Specification<CustomerMonthlyBill> spec = (root, query, cb) ->
                cb.like(root.get("month"), yearPrefix);
        return customerMonthlyBillRepository.findAll(spec);
    }

    public CustomerMonthlyBill updateBill(Long id, Double totalBill, Double undiscountedBill, Double customerPayableBill, Double supplierPayableBill, Double profit) {
        CustomerMonthlyBill bill = customerMonthlyBillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found with id: " + id));

        if (totalBill != null) bill.setTotalBill(totalBill);
        if (undiscountedBill != null) bill.setUndiscountedBill(undiscountedBill);
        if (customerPayableBill != null) bill.setCustomerPayableBill(customerPayableBill);
        if (supplierPayableBill != null) bill.setSupplierPayableBill(supplierPayableBill);
        if (profit != null) bill.setProfit(profit);

        return customerMonthlyBillRepository.save(bill);
    }

    private String normalizeCloudVendor(String accountType) {
        if (!StringUtils.hasText(accountType)) {
            return "AWS"; // Default to AWS if empty
        }
        String lower = accountType.toLowerCase();
        if (lower.contains("ali")) {
            return "Ali";
        }
        if (lower.contains("azure")) {
            return "Azure";
        }
        if (lower.contains("aws")) {
            return "AWS";
        }
        return "AWS"; // Default fallback
    }
}
