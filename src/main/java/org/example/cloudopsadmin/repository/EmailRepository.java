package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailRepository extends JpaRepository<Email, Long>, JpaSpecificationExecutor<Email> {
    Optional<Email> findByEmailAddress(String emailAddress);
    Optional<Email> findByEmailAddressIgnoreCase(String emailAddress);
    Optional<Email> findByEmailInternalId(String emailInternalId);
    boolean existsByEmailAddress(String emailAddress);
    boolean existsByEmailAddressIgnoreCase(String emailAddress);

    @Query("SELECT e.emailInternalId FROM Email e WHERE e.emailInternalId LIKE :pattern ORDER BY e.emailInternalId DESC LIMIT 1")
    Optional<String> findLastEmailInternalId(@Param("pattern") String pattern);
}
