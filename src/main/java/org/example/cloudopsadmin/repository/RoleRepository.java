package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}
