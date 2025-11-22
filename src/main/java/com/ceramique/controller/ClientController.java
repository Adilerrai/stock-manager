package com.ceramique.controller;

import com.ceramique.persistent.enums.CategorieClient;
import com.ceramique.persistent.dto.ClientDTO;
import com.ceramique.persistent.model.Client;
import com.ceramique.service.ClientService;
import com.ceramique.mapper.ClientMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
public class ClientController {

    private final ClientService clientService;
    private final ClientMapper clientMapper;

    public ClientController(ClientService clientService, ClientMapper clientMapper) {
        this.clientService = clientService;
        this.clientMapper = clientMapper;
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

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiverClient(@PathVariable Long id) {
        clientService.desactiverClient(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/activer")
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
