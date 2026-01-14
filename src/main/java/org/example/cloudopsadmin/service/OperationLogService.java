package org.example.cloudopsadmin.service;

import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.entity.OperationLog;
import org.example.cloudopsadmin.repository.OperationLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    @Transactional
    public void log(String operatorEmail,
                    String operatorName,
                    String action,
                    String targetType,
                    String targetId,
                    String description) {
        OperationLog log = new OperationLog();
        log.setOperatorEmail(operatorEmail);
        log.setOperatorName(operatorName);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        operationLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<OperationLog> list(int page,
                                   int pageSize,
                                   String operator,
                                   String targetType,
                                   String action,
                                   String search,
                                   String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder == null ? "DESC" : sortOrder), "id");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        Specification<OperationLog> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();

            if (StringUtils.hasText(operator)) {
                String like = "%" + operator.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("operatorEmail")), like),
                        cb.like(cb.lower(root.get("operatorName")), like)
                ));
            }

            if (StringUtils.hasText(targetType)) {
                predicates.add(cb.equal(root.get("targetType"), targetType));
            }

            if (StringUtils.hasText(action)) {
                predicates.add(cb.equal(root.get("action"), action));
            }

            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("description")), like));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return operationLogRepository.findAll(spec, pageable);
    }
}

