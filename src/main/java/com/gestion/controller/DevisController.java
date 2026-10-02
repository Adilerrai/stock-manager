package com.gestion.controller;

import com.gestion.mapper.CommandeClientMapper;
import com.gestion.mapper.DevisMapper;
import com.gestion.mapper.FactureMapper;
import com.gestion.persistent.dto.CommandeClientDTO;
import com.gestion.persistent.dto.DevisDTO;
import com.gestion.persistent.dto.FactureDTO;
import com.gestion.persistent.enums.StatutDevis;
import com.gestion.persistent.model.CommandeClient;
import com.gestion.persistent.model.Devis;
import com.gestion.persistent.model.Facture;
import com.gestion.service.DevisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/devis")
@CrossOrigin(origins = "*")
public class DevisController {

    private final DevisService devisService;
    private final DevisMapper devisMapper;
    private final CommandeClientMapper commandeClientMapper;
    private final FactureMapper factureMapper;

    public DevisController(DevisService devisService,
                           DevisMapper devisMapper,
                           CommandeClientMapper commandeClientMapper,
                           FactureMapper factureMapper) {
        this.devisService = devisService;
        this.devisMapper = devisMapper;
        this.commandeClientMapper = commandeClientMapper;
        this.factureMapper = factureMapper;
    }

    @PostMapping
    public ResponseEntity<DevisDTO> creerDevis(@RequestBody DevisDTO devisDTO,
                                               @RequestParam(required = false) Long userId) {
        Devis nouveauDevis = devisService.creerDevis(devisDTO, userId);
        return new ResponseEntity<>(devisMapper.toDto(nouveauDevis), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DevisDTO> modifierDevis(@PathVariable Long id,
                                                  @RequestBody DevisDTO devisDTO) {
        Devis modifie = devisService.modifierDevis(id, devisDTO);
        return ResponseEntity.ok(devisMapper.toDto(modifie));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DevisDTO> getDevisById(@PathVariable Long id) {
        Devis devis = devisService.getDevisById(id);
        return ResponseEntity.ok(devisMapper.toDto(devis));
    }

    @GetMapping
    public ResponseEntity<List<DevisDTO>> getAllDevis() {
        List<Devis> devisList = devisService.getAllDevis();
        List<DevisDTO> dtos = devisList.stream()
                .map(devisMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<DevisDTO>> getDevisByClient(@PathVariable Long clientId) {
        List<Devis> devisList = devisService.getDevisByClient(clientId);
        List<DevisDTO> dtos = devisList.stream()
                .map(devisMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<DevisDTO>> getDevisByStatut(@PathVariable StatutDevis statut) {
        List<Devis> devisList = devisService.getDevisByStatut(statut);
        List<DevisDTO> dtos = devisList.stream()
                .map(devisMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<DevisDTO> changerStatut(@PathVariable Long id,
                                                  @RequestParam StatutDevis statut) {
        Devis devis = devisService.changerStatut(id, statut);
        return ResponseEntity.ok(devisMapper.toDto(devis));
    }

    @PostMapping("/{id}/convertir-commande")
    public ResponseEntity<CommandeClientDTO> convertirEnCommande(@PathVariable Long id,
                                                                 @RequestParam(required = false) Long userId) {
        CommandeClient commande = devisService.transformerEnCommandeClient(id, userId);
        return new ResponseEntity<>(commandeClientMapper.toDto(commande), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/convertir-facture")
    public ResponseEntity<FactureDTO> convertirEnFacture(@PathVariable Long id,
                                                         @RequestParam(required = false) Long userId) {
        Facture facture = devisService.transformerEnFacture(id, userId);
        return new ResponseEntity<>(factureMapper.toDto(facture), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerDevis(@PathVariable Long id) {
        devisService.supprimerDevis(id);
        return ResponseEntity.noContent().build();
    }
}
