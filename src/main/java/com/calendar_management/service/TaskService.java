package com.calendar_management.service;

import com.calendar_management.dto.TaskDTO;
import com.calendar_management.model.Projeto;
import com.calendar_management.model.Task;
import com.calendar_management.repository.ProjetoRepository;
import com.calendar_management.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjetoRepository projetoRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository, ProjetoRepository projetoRepository) {
        this.taskRepository = taskRepository;
        this.projetoRepository = projetoRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findAll() {
        return taskRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findByProjetoId(Long projetoId) {
        return taskRepository.findByProjetoId(projetoId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDTO findById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task não encontrada com id: " + id));
        return convertToDTO(task);
    }

    @Transactional(readOnly = true)
    public TaskDTO findByTagTask(String tagTask) {
        Task task = taskRepository.findByTagTask(tagTask)
                .orElseThrow(() -> new RuntimeException("Task não encontrada com tag: " + tagTask));
        return convertToDTO(task);
    }

    @Transactional
    public TaskDTO save(TaskDTO taskDTO) {
        if (taskRepository.existsByTagTask(taskDTO.getTagTask())) {
            throw new RuntimeException("Já existe uma task com a tag: " + taskDTO.getTagTask());
        }
        
        Projeto projeto = projetoRepository.findById(taskDTO.getIdProjeto())
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado com id: " + taskDTO.getIdProjeto()));
        
        Task task = new Task();
        task.setNomeTask(taskDTO.getNomeTask());
        task.setDescricaoTaks(taskDTO.getDescricaoTask());
        task.setProjeto(projeto);
        task.setTagTask(taskDTO.getTagTask());
        task.setDataInicio(taskDTO.getDataInicio());
        task.setDataFim(taskDTO.getDataFim());
        task.setIsAtivo(taskDTO.getIsAtivo());
        
        task = taskRepository.save(task);
        return convertToDTO(task);
    }

    @Transactional
    public TaskDTO update(Long id, TaskDTO taskDTO) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task não encontrada com id: " + id));
        
        // Check if tag is being changed and if the new tag already exists
        if (!existingTask.getTagTask().equals(taskDTO.getTagTask()) && 
            taskRepository.existsByTagTask(taskDTO.getTagTask())) {
            throw new RuntimeException("Já existe uma task com a tag: " + taskDTO.getTagTask());
        }
        
        Projeto projeto = projetoRepository.findById(taskDTO.getIdProjeto())
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado com id: " + taskDTO.getIdProjeto()));
        
        Task task = new Task();
        task.setId(id);
        task.setNomeTask(taskDTO.getNomeTask());
        task.setDescricaoTaks(taskDTO.getDescricaoTask());
        task.setProjeto(projeto);
        task.setTagTask(taskDTO.getTagTask());
        task.setDataInicio(taskDTO.getDataInicio());
        task.setDataFim(taskDTO.getDataFim());
        task.setIsAtivo(taskDTO.getIsAtivo());
        
        task = taskRepository.save(task);
        return convertToDTO(task);
    }

    @Transactional
    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new RuntimeException("Task não encontrada com id: " + id);
        }
        taskRepository.deleteById(id);
    }

    private TaskDTO convertToDTO(Task task) {
        return new TaskDTO(
                task.getId(),
                task.getNomeTask(),
                task.getDescricaoTaks(),
                task.getProjeto().getId(),
                task.getTagTask(),
                task.getDataInicio(),
                task.getDataFim(),
                task.getIsAtivo()
        );
    }
}