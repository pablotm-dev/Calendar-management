package com.calendar_management.service;

import com.calendar_management.dto.ClienteDTO;
import com.calendar_management.model.Cliente;
import com.calendar_management.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Autowired
    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO> findAll() {
        return clienteRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClienteDTO findById(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado com id: " + id));
        return convertToDTO(cliente);
    }

    @Transactional
    public ClienteDTO save(ClienteDTO clienteDTO) {
        Cliente cliente = convertToEntity(clienteDTO);
        cliente = clienteRepository.save(cliente);
        return convertToDTO(cliente);
    }

    @Transactional
    public ClienteDTO update(Long id, ClienteDTO clienteDTO) {
        if (!clienteRepository.existsById(id)) {
            throw new RuntimeException("Cliente não encontrado com id: " + id);
        }
        
        Cliente cliente = convertToEntity(clienteDTO);
        cliente.setId(id);
        cliente = clienteRepository.save(cliente);
        return convertToDTO(cliente);
    }

    @Transactional
    public void delete(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new RuntimeException("Cliente não encontrado com id: " + id);
        }
        clienteRepository.deleteById(id);
    }

    private ClienteDTO convertToDTO(Cliente cliente) {
        return new ClienteDTO(
                cliente.getId(),
                cliente.getNomeCliente()
        );
    }

    private Cliente convertToEntity(ClienteDTO clienteDTO) {
        return new Cliente(
                clienteDTO.getId(),
                clienteDTO.getNomeCliente()
        );
    }
}