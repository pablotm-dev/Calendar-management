package com.calendar_management.repository;

import com.calendar_management.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjetoId(Long projetoId);
    Optional<Task> findByTagTask(String tagTask);
    boolean existsByTagTask(String tagTask);
}