package com.calendar_management.controller;

import com.calendar_management.dto.TaskDTO;
import com.calendar_management.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskDTO>> findAll() {
        List<TaskDTO> tasks = taskService.findAll();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/projeto/{projetoId}")
    public ResponseEntity<List<TaskDTO>> findByProjetoId(@PathVariable Long projetoId) {
        List<TaskDTO> tasks = taskService.findByProjetoId(projetoId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> findById(@PathVariable Long id) {
        try {
            TaskDTO task = taskService.findById(id);
            return ResponseEntity.ok(task);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/tag/{tagTask}")
    public ResponseEntity<TaskDTO> findByTagTask(@PathVariable String tagTask) {
        try {
            TaskDTO task = taskService.findByTagTask(tagTask);
            return ResponseEntity.ok(task);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TaskDTO taskDTO) {
        try {
            TaskDTO savedTask = taskService.save(taskDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TaskDTO taskDTO) {
        try {
            TaskDTO updatedTask = taskService.update(id, taskDTO);
            return ResponseEntity.ok(updatedTask);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            taskService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}