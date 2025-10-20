package com.calendar_management.service;

import com.calendar_management.dto.TaskDTO;
import com.calendar_management.model.Projeto;
import com.calendar_management.model.Task;
import com.calendar_management.repository.ProjetoRepository;
import com.calendar_management.repository.TaskRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjetoRepository projetoRepository;

    // Tag fallback caso a tag do evento não exista previamente
    private static final String GENERIC_TAG = "#GENERICO";

    /**
     * Regex: pega a PRIMEIRA palavra iniciada por # no começo do summary (aceita acentos/Unicode).
     * - ^\\s*      => ignora espaços no início
     * - (#[\\p{L}\\p{N}_-]+) => # seguido de letras (Unicode), números, _ ou -
     */
    private static final Pattern LEADING_TAG =
            Pattern.compile("^\\s*(#[\\p{L}\\p{N}_-]+)", Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * Cache em memória de resolução de tags:
     *  chave = TAG NORMALIZADA (ver normalizeTag)
     *  valor = Task
     */
    private final ConcurrentHashMap<String, Task> tagCache = new ConcurrentHashMap<>();

    @Autowired
    public TaskService(TaskRepository taskRepository, ProjetoRepository projetoRepository) {
        this.taskRepository = taskRepository;
        this.projetoRepository = projetoRepository;
    }

    // =======================
    // Bootstrap do cache
    // =======================

    @PostConstruct
    @Transactional(readOnly = true)
    public void preloadCache() {
        List<Task> all = taskRepository.findAll();
        for (Task t : all) {
            String norm = normalizeTag(t.getTagTask());
            if (norm != null) {
                tagCache.put(norm, t);
            }
        }
        // Garante que #GENERICO existe no cache
        Task gen = tagCache.get(normalizeTag(GENERIC_TAG));
        if (gen == null) {
            throw new IllegalStateException(
                    "Task padrão " + GENERIC_TAG + " não encontrada. Cadastre-a antes de iniciar o serviço."
            );
        }
    }

    // =======================
    // CRUD existentes
    // =======================

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

        Task saved = taskRepository.save(task);

        // Atualiza cache com a chave NORMALIZADA
        String norm = normalizeTag(saved.getTagTask());
        if (norm != null) {
            tagCache.put(norm, saved);
        }

        return convertToDTO(saved);
    }

    @Transactional
    public TaskDTO update(Long id, TaskDTO taskDTO) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task não encontrada com id: " + id));

        if (!existingTask.getTagTask().equals(taskDTO.getTagTask()) &&
                taskRepository.existsByTagTask(taskDTO.getTagTask())) {
            throw new RuntimeException("Já existe uma task com a tag: " + taskDTO.getTagTask());
        }

        Projeto projeto = projetoRepository.findById(taskDTO.getIdProjeto())
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado com id: " + taskDTO.getIdProjeto()));

        existingTask.setNomeTask(taskDTO.getNomeTask());
        existingTask.setDescricaoTaks(taskDTO.getDescricaoTask());
        existingTask.setProjeto(projeto);
        existingTask.setTagTask(taskDTO.getTagTask());
        existingTask.setDataInicio(taskDTO.getDataInicio());
        existingTask.setDataFim(taskDTO.getDataFim());
        existingTask.setIsAtivo(taskDTO.getIsAtivo());

        Task saved = taskRepository.save(existingTask);

        // Remove entradas antigas da mesma Task (caso a tag tenha mudado)
        tagCache.entrySet().removeIf(e -> e.getValue().getId().equals(saved.getId()));

        // Reinsere a nova chave normalizada
        String norm = normalizeTag(saved.getTagTask());
        if (norm != null) {
            tagCache.put(norm, saved);
        }

        return convertToDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new RuntimeException("Task não encontrada com id: " + id);
        }
        // Limpa entradas dessa Task no cache
        tagCache.entrySet().removeIf(e -> e.getValue().getId().equals(id));
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

    // =======================
    // NOVOS MÉTODOS (tag/extração/otimização)
    // =======================

    /** Extrai a tag líder do summary (ex.: "#ACCIO_PROJETO ..."). Se não houver, retorna null. */
    @Transactional(readOnly = true)
    public String extractLeadingTag(String summary) {
        if (summary == null) return null;
        Matcher m = LEADING_TAG.matcher(summary);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    /** Normaliza a tag: garante que começa com '#', faz trim; (não altera maiúsc/minúsc). */
    public String normalizeTag(String tag) {
        if (tag == null) return null;
        String t = tag.trim();
        if (t.isEmpty()) return null;
        if (!t.startsWith("#")) t = "#" + t;
        return t;
    }

    /** Atalho: extrai e já normaliza a tag inicial do summary (ou null). */
    @Transactional(readOnly = true)
    public String normalizedLeadingTag(String summary) {
        return normalizeTag(extractLeadingTag(summary));
    }

    /** Retorna a Task de fallback (#GENERICO). */
    @Transactional(readOnly = true)
    public Task getGenericTask() {
        Task cached = tagCache.get(normalizeTag(GENERIC_TAG));
        if (cached != null) return cached;
        Task gen = taskRepository.findByTagTask(GENERIC_TAG)
                .orElseThrow(() -> new IllegalStateException("Task padrão " + GENERIC_TAG + " não encontrada."));
        tagCache.put(normalizeTag(GENERIC_TAG), gen);
        return gen;
    }

    /** Resolve uma Task por tag; se não existir, retorna a Task "#GENERICO". */
    @Transactional(readOnly = true)
    public Task resolveByTagOrGeneric(String possibleTag) {
        String normalized = normalizeTag(possibleTag);
        if (normalized != null) {
            Task fromCache = tagCache.get(normalized);
            if (fromCache != null) return fromCache;

            Optional<Task> byTag = taskRepository.findByTagTask(normalized);
            if (byTag.isPresent()) {
                tagCache.put(normalized, byTag.get());
                return byTag.get();
            }
        }
        return getGenericTask();
    }

    /**
     * Resolve um conjunto de tags em lote, minimizando queries:
     * - usa o cache para as já conhecidas (chave = NORMALIZADA)
     * - busca as faltantes em UMA única query (IN) com as tags NORMALIZADAS
     * - popula o cache e retorna um mapa {tagNormalizada -> Task}
     */
    @Transactional(readOnly = true)
    public Map<String, Task> resolveTagsBulk(Collection<String> rawTags) {
        Map<String, Task> result = new HashMap<>();
        if (rawTags == null || rawTags.isEmpty()) return result;

        // Normaliza e remove nulos/vazios
        List<String> normalized = rawTags.stream()
                .map(this::normalizeTag)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (normalized.isEmpty()) return result;

        // 1) pega do cache
        List<String> missing = new ArrayList<>();
        for (String t : normalized) {
            Task cached = tagCache.get(t);
            if (cached != null) {
                result.put(t, cached);
            } else {
                missing.add(t);
            }
        }

        // 2) busca em lote as que faltam
        if (!missing.isEmpty()) {
            List<Task> fetched = taskRepository.findByTagTaskIn(missing);
            for (Task t : fetched) {
                String key = normalizeTag(t.getTagTask());
                if (key != null) {
                    tagCache.put(key, t);
                    result.put(key, t);
                }
            }
        }

        return result;
    }
}
