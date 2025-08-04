package com.calendar_management.service;

import com.calendar_management.dto.ProjetoDTO;
import com.calendar_management.model.Cliente;
import com.calendar_management.model.Projeto;
import com.calendar_management.repository.ClienteRepository;
import com.calendar_management.repository.ProjetoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final ClienteRepository clienteRepository;

    @Autowired
    public ProjetoService(ProjetoRepository projetoRepository, ClienteRepository clienteRepository) {
        this.projetoRepository = projetoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjetoDTO> findAll() {
        return projetoRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjetoDTO> findByClienteId(Long clienteId) {
        return projetoRepository.findByClienteId(clienteId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjetoDTO findById(Long id) {
        Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado com id: " + id));
        return convertToDTO(projeto);
    }

    @Transactional
    public ProjetoDTO save(ProjetoDTO projetoDTO) {
        Cliente cliente = clienteRepository.findById(projetoDTO.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado com id: " + projetoDTO.getIdCliente()));
        
        Projeto projeto = new Projeto();
        projeto.setNomeProjeto(projetoDTO.getNomeProjeto());
        projeto.setCliente(cliente);
        
        projeto = projetoRepository.save(projeto);
        return convertToDTO(projeto);
    }

    @Transactional
    public ProjetoDTO update(Long id, ProjetoDTO projetoDTO) {
        if (!projetoRepository.existsById(id)) {
            throw new RuntimeException("Projeto não encontrado com id: " + id);
        }
        
        Cliente cliente = clienteRepository.findById(projetoDTO.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado com id: " + projetoDTO.getIdCliente()));
        
        Projeto projeto = new Projeto();
        projeto.setId(id);
        projeto.setNomeProjeto(projetoDTO.getNomeProjeto());
        projeto.setCliente(cliente);
        
        projeto = projetoRepository.save(projeto);
        return convertToDTO(projeto);
    }

    @Transactional
    public void delete(Long id) {
        if (!projetoRepository.existsById(id)) {
            throw new RuntimeException("Projeto não encontrado com id: " + id);
        }
        projetoRepository.deleteById(id);
    }

    private ProjetoDTO convertToDTO(Projeto projeto) {
        return new ProjetoDTO(
                projeto.getId(),
                projeto.getNomeProjeto(),
                projeto.getCliente().getId()
        );
    }
}