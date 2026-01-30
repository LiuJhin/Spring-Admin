package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.CloudProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CloudProviderRepository extends JpaRepository<CloudProvider, Long> {
}
