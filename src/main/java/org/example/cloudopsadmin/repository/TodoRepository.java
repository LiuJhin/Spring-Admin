package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {
}
