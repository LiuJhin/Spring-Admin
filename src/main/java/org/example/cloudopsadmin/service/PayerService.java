package org.example.cloudopsadmin.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.entity.Account;
import org.example.cloudopsadmin.entity.Payer;
import org.example.cloudopsadmin.repository.PayerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PayerService {

    private final PayerRepository payerRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Simple counter for demo purposes. In production, consider using a database sequence or Redis.
    private static final AtomicInteger sequence = new AtomicInteger(1); 
    private static final Pattern SIGNIN_URL_PATTERN = Pattern.compile("^https://\\d{12}\\.signin\\.aws\\.amazon\\.com/.*$");

    @Transactional(readOnly = true)
    public Page<Payer> getPayerList(int page, int pageSize, String search, String label, String sortBy, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder == null ? "ASC" : sortOrder), 
                            sortBy == null || sortBy.isEmpty() ? "id" : sortBy);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        Specification<Payer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String likePattern = "%" + search.toLowerCase() + "%";
                
                // Payer fields
                Predicate payerName = cb.like(cb.lower(root.get("payerName")), likePattern);
                Predicate payerId = cb.like(root.get("payerId"), likePattern);
                Predicate contactEmail = cb.like(cb.lower(root.get("contactEmail")), likePattern);
                
                // Account fields
                Join<Payer, Account> accountsJoin = root.join("accounts", JoinType.LEFT);
                Predicate accountName = cb.like(cb.lower(accountsJoin.get("accountName")), likePattern);
                Predicate accountBoundEmail = cb.like(cb.lower(accountsJoin.get("boundEmail")), likePattern);
                Predicate accountMonitorEmail = cb.like(cb.lower(accountsJoin.get("monitorEmail")), likePattern);

                predicates.add(cb.or(payerName, payerId, contactEmail, accountName, accountBoundEmail, accountMonitorEmail));
            }

            if (StringUtils.hasText(label)) {
                Join<Payer, Account> accountsJoin = root.join("accounts", JoinType.INNER); // Use INNER join to filter Payers with matching Accounts
                Join<Account, String> labelsJoin = accountsJoin.join("labels", JoinType.INNER);
                predicates.add(cb.equal(labelsJoin, label));
            }
            
            query.distinct(true); // Prevent duplicates when joining
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return payerRepository.findAll(spec, pageable);
    }

    @Transactional
    public Payer createPayer(Payer payer) {
        // Validate Payer ID uniqueness
        if (payerRepository.existsByPayerId(payer.getPayerId())) {
            throw new IllegalArgumentException("Payer ID already exists");
        }

        // Validate Sign-in URL format
        if (!SIGNIN_URL_PATTERN.matcher(payer.getSigninUrl()).matches()) {
            throw new IllegalArgumentException("Invalid Sign-in URL format. Must be https://[12-digit-id].signin.aws.amazon.com/...");
        }

        // Generate Internal ID
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String internalId = String.format("payer_%s_%03d", datePart, sequence.getAndIncrement());
        payer.setPayerInternalId(internalId);

        // Encrypt Password
        payer.setPassword(passwordEncoder.encode(payer.getPassword()));

        return payerRepository.save(payer);
    }
}
