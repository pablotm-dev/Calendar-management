package com.calendar_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_tag_task", columnList = "tag_task", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nome_task", nullable = false)
    private String nomeTask;
    
    @Column(name = "descricao_taks")
    private String descricaoTaks;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projeto", nullable = false)
    private Projeto projeto;
    
    @Column(name = "tag_task", nullable = false, unique = true)
    private String tagTask;
    
    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;
    
    @Column(name = "data_fim")
    private LocalDateTime dataFim;
    
    @Column(name = "is_ativo")
    private Boolean isAtivo;
}