package com.calendar_management.repository;

import com.calendar_management.model.Projeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, Long> {
    List<Projeto> findByClienteId(Long clienteId);
}