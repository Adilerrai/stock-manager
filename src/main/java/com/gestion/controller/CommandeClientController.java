package com.gestion.controller;

import com.gestion.mapper.CommandeClientMapper;
import com.gestion.persistent.dto.CommandeClientDTO;
import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.model.CommandeClient;
import com.gestion.service.CommandeClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ventes")
public class CommandeClientController {

    private final CommandeClientService commandeClientService;
    private final CommandeClientMapper commandeClientMapper;

    public CommandeClientController(CommandeClientService commandeClientService,
                                   CommandeClientMapper commandeClientMapper) {
        this.commandeClientService = commandeClientService;
        this.commandeClientMapper = commandeClientMapper;
    }

    @PostMapping("/create")
    public ResponseEntity<CommandeClientDTO> createVente(@RequestBody CommandeClientDTO commandeDTO) {
        CommandeClient commande = commandeClientService.createCommandeClient(commandeDTO);
        return ResponseEntity.ok(commandeClientMapper.toDto(commande));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CommandeClientDTO>> getAllVentes() {
        List<CommandeClient> commandes = commandeClientService.getAllCommandesClient();
        List<CommandeClientDTO> commandeDTOs = commandes.stream()
                .map(commandeClientMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commandeDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommandeClientDTO> getVenteById(@PathVariable Long id) {
        CommandeClient commande = commandeClientService.getCommandeClientById(id);
        return ResponseEntity.ok(commandeClientMapper.toDto(commande));
    }

    @PutMapping("/{id}/statut")
    public ResponseEntity<CommandeClientDTO> updateStatut(@PathVariable Long id, 
                                                         @RequestParam StatutCommandeClient statut) {
        CommandeClient commande = commandeClientService.updateStatut(id, statut);
        return ResponseEntity.ok(commandeClientMapper.toDto(commande));
    }

    @PutMapping("/{id}/confirmer")
    public ResponseEntity<CommandeClientDTO> confirmerVente(@PathVariable Long id) {
        CommandeClient commande = commandeClientService.updateStatut(id, StatutCommandeClient.CONFIRMEE);
        return ResponseEntity.ok(commandeClientMapper.toDto(commande));
    }

    @PutMapping("/{id}/annuler")
    public ResponseEntity<CommandeClientDTO> annulerVente(@PathVariable Long id) {
        CommandeClient commande = commandeClientService.updateStatut(id, StatutCommandeClient.ANNULEE);
        return ResponseEntity.ok(commandeClientMapper.toDto(commande));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<CommandeClientDTO>> getVentesByStatut(@PathVariable StatutCommandeClient statut) {
        List<CommandeClient> commandes = commandeClientService.getCommandesByStatut(statut);
        List<CommandeClientDTO> commandeDTOs = commandes.stream()
                .map(commandeClientMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commandeDTOs);
    }
}
