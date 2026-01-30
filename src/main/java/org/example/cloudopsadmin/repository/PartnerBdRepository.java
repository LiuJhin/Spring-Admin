package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.PartnerBd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerBdRepository extends JpaRepository<PartnerBd, Long> {
}
