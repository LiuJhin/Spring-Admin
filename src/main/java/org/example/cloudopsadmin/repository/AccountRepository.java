package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {
    boolean existsByUid(String uid);

    Optional<Account> findByUid(String uid);

    Optional<Account> findByAccountInternalId(String accountInternalId);

    @Query("SELECT a.accountInternalId FROM Account a WHERE a.accountInternalId LIKE :pattern ORDER BY a.accountInternalId DESC LIMIT 1")
    Optional<String> findLastAccountInternalId(@Param("pattern") String pattern);

    int countByBoundCreditCardMasked(String boundCreditCardMasked);

    boolean existsByLinkedEmail(org.example.cloudopsadmin.entity.Email linkedEmail);

    boolean existsByLinkedEmailAndUidNot(org.example.cloudopsadmin.entity.Email linkedEmail, String uid);
}
