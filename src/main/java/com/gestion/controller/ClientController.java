package com.gestion.controller;

import com.gestion.persistent.enums.CategorieClient;
import com.gestion.persistent.dto.ClientDTO;
import com.gestion.persistent.model.Client;
import com.gestion.service.ClientService;
import com.gestion.mapper.ClientMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import com.gestion.persistent.dto.ClientSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;
    private final ClientMapper clientMapper;

    public ClientController(ClientService clientService, ClientMapper clientMapper) {
        this.clientService = clientService;
        this.clientMapper = clientMapper;
    }

    @PostMapping("/search")
    public ResponseEntity<Page<ClientDTO>> searchClients(@RequestBody ClientSearchCriteria criteria, Pageable pageable) {
        Page<Client> page = clientService.searchClients(criteria, pageable);
        return ResponseEntity.ok(page.map(clientMapper::toDto));
    }

    @PostMapping
    public ResponseEntity<ClientDTO> creerClient(@RequestBody ClientDTO clientDTO) {
        Client entity = clientMapper.toEntity(clientDTO);
        Client nouveauClient = clientService.creerClient(entity);
        return new ResponseEntity<>(clientMapper.toDto(nouveauClient), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> modifierClient(@PathVariable Long id, @RequestBody ClientDTO clientDTO) {
        Client entity = clientMapper.toEntity(clientDTO);
        entity.setId(id); // ensure id is set
        Client clientModifie = clientService.modifierClient(id, entity);
        return ResponseEntity.ok(clientMapper.toDto(clientModifie));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getClient(@PathVariable Long id) {
        Client client = clientService.getClientById(id);
        return ResponseEntity.ok(clientMapper.toDto(client));
    }

    @GetMapping
    public ResponseEntity<List<ClientDTO>> getAllClients() {
        List<Client> clients = clientService.getAllClients();
        List<ClientDTO> dtos = clients.stream().map(clientMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiverClient(@PathVariable Long id) {
        clientService.desactiverClient(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/activer")
    public ResponseEntity<Void> activerClient(@PathVariable Long id) {
        clientService.activerClient(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/depassement-credit")
    public ResponseEntity<List<ClientDTO>> getClientsAvecDepassementCredit() {
        List<Client> clients = clientService.getClientsAvecDepassementCredit();
        List<ClientDTO> dtos = clients.stream().map(clientMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}

