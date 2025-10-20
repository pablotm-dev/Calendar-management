package com.calendar_management.repository;

import com.calendar_management.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("select t from Task t where t.projeto.id = :projetoId")
    List<Task> findByProjetoId(Long projetoId);

    Optional<Task> findByTagTask(String tagTask);

    boolean existsByTagTask(String tagTask);

    @Query("select t from Task t where t.tagTask in :tags")
    List<Task> findByTagTaskIn(Collection<String> tags);
}
