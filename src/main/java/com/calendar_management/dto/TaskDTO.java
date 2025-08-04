package com.calendar_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private Long id;
    private String nomeTask;
    private String descricaoTask;
    private Long idProjeto;
    private String tagTask;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Boolean isAtivo;
}