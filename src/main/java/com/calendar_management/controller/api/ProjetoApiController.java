package com.calendar_management.controller.api;

import com.calendar_management.dto.ProjetoDTO;
import com.calendar_management.service.ProjetoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API Controller for Projeto endpoints with /api prefix
 * This controller maps to /api/projetos and forwards requests to ProjetoService
 */
@RestController
@RequestMapping("/api/projetos")
public class ProjetoApiController {

    private final ProjetoService projetoService;

    @Autowired
    public ProjetoApiController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    @GetMapping
    public ResponseEntity<List<ProjetoDTO>> findAll() {
        List<ProjetoDTO> projetos = projetoService.findAll();
        return ResponseEntity.ok(projetos);
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<ProjetoDTO>> findByClienteId(@PathVariable Long clienteId) {
        List<ProjetoDTO> projetos = projetoService.findByClienteId(clienteId);
        return ResponseEntity.ok(projetos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjetoDTO> findById(@PathVariable Long id) {
        try {
            ProjetoDTO projeto = projetoService.findById(id);
            return ResponseEntity.ok(projeto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ProjetoDTO> create(@RequestBody ProjetoDTO projetoDTO) {
        try {
            ProjetoDTO savedProjeto = projetoService.save(projetoDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProjeto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjetoDTO> update(@PathVariable Long id, @RequestBody ProjetoDTO projetoDTO) {
        try {
            ProjetoDTO updatedProjeto = projetoService.update(id, projetoDTO);
            return ResponseEntity.ok(updatedProjeto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            projetoService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}